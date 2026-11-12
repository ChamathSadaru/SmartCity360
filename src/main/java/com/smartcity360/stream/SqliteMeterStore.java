package com.smartcity360.stream;

import com.sun.jna.platform.win32.Crypt32Util;
import com.sun.jna.platform.win32.Win32Exception;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.sun.jna.platform.win32.WinCrypt.CRYPTPROTECT_UI_FORBIDDEN;

final class SqliteMeterStore {
    record StoredReading(byte[] payload, byte[] tokenHash, byte[] protectedToken) {}

    private final String databaseUrl;

    SqliteMeterStore(Path database) {
        Path file = database.toAbsolutePath().normalize();
        databaseUrl = "jdbc:sqlite:" + file;
        try {
            Files.createDirectories(file.getParent());
            try (var connection = connect(); var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS meter_readings (
                            reading_id INTEGER PRIMARY KEY,
                            meter_id TEXT NOT NULL,
                            recorded_at TEXT NOT NULL,
                            encrypted_payload BLOB NOT NULL,
                            token_hash BLOB,
                            protected_token BLOB
                        )
                        """);
                ensureBlobColumn(connection, "token_hash");
                ensureBlobColumn(connection, "protected_token");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS metering_key (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            protected_key BLOB NOT NULL
                        )
                        """);
            }
        } catch (IOException | SQLException error) {
            throw new IllegalStateException("Could not open the meter database at " + file + ".", error);
        }
    }

    List<MeteringSession.ReadingRow> list() {
        var readings = new ArrayList<MeteringSession.ReadingRow>();
        try (var connection = connect();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     SELECT reading_id, meter_id, recorded_at FROM meter_readings ORDER BY reading_id
                     """)) {
            while (rows.next()) {
                readings.add(new MeteringSession.ReadingRow(
                        rows.getLong(1), rows.getString(2), Instant.parse(rows.getString(3))));
            }
            return List.copyOf(readings);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not load meter readings from SQLite.", error);
        }
    }

    void clearReadings() {
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM meter_readings");
        } catch (SQLException error) {
            throw new IllegalStateException("Could not clear the saved meter readings.", error);
        }
    }

    private static void ensureBlobColumn(Connection connection, String name) throws SQLException {
        if (hasColumn(connection, name)) {
            return;
        }
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE meter_readings ADD COLUMN " + name + " BLOB");
        } catch (SQLException error) {
            if (!hasColumn(connection, name)) {
                throw error;
            }
        }
    }

    private static boolean hasColumn(Connection connection, String name) throws SQLException {
        try (var statement = connection.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(meter_readings)")) {
            while (columns.next()) {
                if (name.equals(columns.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    MeteringSession.ReadingRow insert(String meterId, Instant recordedAt, byte[] ciphertext, byte[] tokenHash, String token) {
        try (var connection = connect();
             var statement = connection.prepareStatement("""
                     INSERT INTO meter_readings (meter_id, recorded_at, encrypted_payload, token_hash, protected_token)
                     VALUES (?, ?, ?, ?, ?) RETURNING reading_id
                     """)) {
            statement.setString(1, meterId);
            statement.setString(2, recordedAt.toString());
            statement.setBytes(3, ciphertext);
            statement.setBytes(4, tokenHash);
            statement.setBytes(5, tokenHash == null ? null : protectToken(token));
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("SQLite did not return a reading ID.");
                }
                return new MeteringSession.ReadingRow(result.getLong(1), meterId, recordedAt);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not save the reading to SQLite.", error);
        }
    }

    StoredReading loadReading(long id) {
        try (var connection = connect();
             var statement = connection.prepareStatement(
                     "SELECT encrypted_payload, token_hash, protected_token FROM meter_readings WHERE reading_id = ?")) {
            statement.setLong(1, id);
            try (var row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new IllegalStateException("The selected reading could not be found.");
                }
                return new StoredReading(row.getBytes(1), row.getBytes(2), row.getBytes(3));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read the selected reading from SQLite.", error);
        }
    }

    void rememberToken(long id, String token) {
        try (var connection = connect();
             var statement = connection.prepareStatement(
                     "UPDATE meter_readings SET protected_token = ? WHERE reading_id = ? AND protected_token IS NULL")) {
            statement.setBytes(1, protectToken(token));
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not remember the access token for this reading.", error);
        }
    }

    private static byte[] protectToken(String token) {
        byte[] raw = token.getBytes(StandardCharsets.UTF_8);
        try {
            return Crypt32Util.cryptProtectData(raw, CRYPTPROTECT_UI_FORBIDDEN);
        } catch (Win32Exception error) {
            throw new IllegalStateException("Windows could not protect the access token.", error);
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    static String recoverToken(byte[] protectedToken) {
        try {
            byte[] raw = Crypt32Util.cryptUnprotectData(protectedToken, CRYPTPROTECT_UI_FORBIDDEN);
            try {
                return new String(raw, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(raw, (byte) 0);
            }
        } catch (Win32Exception error) {
            throw new IllegalStateException("Windows could not open the saved access token.", error);
        }
    }

    SecretKey loadKey() {
        try (var connection = connect()) {
            byte[] protectedKey = findProtectedKey(connection);
            if (protectedKey == null) {
                initializeKey(connection);
                protectedKey = findProtectedKey(connection);
            }
            if (protectedKey == null) {
                throw new IllegalStateException("This database has readings but its encryption key is missing.");
            }
            byte[] raw = Crypt32Util.cryptUnprotectData(protectedKey, CRYPTPROTECT_UI_FORBIDDEN);
            try {
                return new SecretKeySpec(raw, "AES");
            } finally {
                Arrays.fill(raw, (byte) 0);
            }
        } catch (Win32Exception error) {
            throw new IllegalStateException("Open the meter database using its original Windows account.", error);
        } catch (SQLException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Could not prepare the meter database encryption key.", error);
        }
    }

    private static byte[] findProtectedKey(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT protected_key FROM metering_key WHERE id = 1")) {
            return result.next() ? result.getBytes(1) : null;
        }
    }

    private static void initializeKey(Connection connection) throws SQLException, NoSuchAlgorithmException {
        var generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        byte[] raw = generator.generateKey().getEncoded();
        try (var statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO metering_key (id, protected_key)
                SELECT 1, ? WHERE NOT EXISTS (SELECT 1 FROM meter_readings)
                """)) {
            statement.setBytes(1, Crypt32Util.cryptProtectData(raw, CRYPTPROTECT_UI_FORBIDDEN));
            statement.executeUpdate();
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }
}
