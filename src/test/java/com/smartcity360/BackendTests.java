package com.smartcity360;

import com.smartcity360.complaint.Complaint;
import com.smartcity360.complaint.ComplaintHandler;
import com.smartcity360.lighting.LightConfigurationFactory;
import com.smartcity360.lighting.SampleStreetlights;
import com.smartcity360.lighting.StreetLight;
import com.smartcity360.policy.PolicyContext;
import com.smartcity360.policy.PolicyParser;
import com.smartcity360.stream.AuthenticationDecorator;
import com.smartcity360.stream.BasicMeterStream;
import com.smartcity360.stream.CompressionDecorator;
import com.smartcity360.stream.EncryptionDecorator;
import com.smartcity360.stream.MeteringEvent;
import com.smartcity360.stream.MeteringSession;
import com.smartcity360.stream.MeterStream;
import com.smartcity360.zone.Building;
import com.smartcity360.zone.CityComponent;
import com.smartcity360.zone.ZoneGroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.KeyGenerator;

import static com.smartcity360.stream.MeteringEvent.Step.COMPRESS;
import static com.smartcity360.stream.MeteringEvent.Step.DECOMPRESS;
import static com.smartcity360.stream.MeteringEvent.Step.DECRYPT;
import static com.smartcity360.stream.MeteringEvent.Step.ENCRYPT;
import static com.smartcity360.stream.MeteringEvent.Step.TOKEN_CHECK;
import static com.smartcity360.zone.ZoneGroup.ZoneType.CITY;
import static com.smartcity360.zone.ZoneGroup.ZoneType.DISTRICT;
import static com.smartcity360.zone.ZoneGroup.ZoneType.STREET;

public final class BackendTests {
    public static void main(String[] args) throws GeneralSecurityException, IOException, SQLException {
        testStream();
        System.out.println("PASS Decorator");
        testMeteringSession();
        System.out.println("PASS SQLite meter history");
        testZones();
        System.out.println("PASS Composite");
        testComplaints();
        System.out.println("PASS Chain of Responsibility");
        testLighting();
        System.out.println("PASS Flyweight");
        testPolicies();
        System.out.println("PASS Interpreter");
    }

    private static void testStream() throws GeneralSecurityException {
        String token = "test-token";
        byte[] reading = "M-42,17 kWh\n".repeat(80).getBytes(StandardCharsets.UTF_8);
        var storage = new BasicMeterStream();
        var compression = new CompressionDecorator(storage);
        var generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        var encryption = new EncryptionDecorator(storage, generator.generateKey());
        var authentication = new AuthenticationDecorator(storage, token);
        MeterStream combined = new AuthenticationDecorator(new CompressionDecorator(encryption), token);

        for (MeterStream stream : List.of(storage, compression, encryption, authentication, combined)) {
            stream.write(reading, token);
            check(Arrays.equals(reading, stream.read(token)), "Each wrapper must support round trips.");
        }
        compression.write(reading, token);
        check(storage.storedBytes() < reading.length, "GZIP should shrink repeated sample readings.");
        combined.write(reading, token);
        byte[] encrypted = storage.read(token);
        combined.write(reading, token);
        byte[] secondWrite = storage.read(token);
        check(!Arrays.equals(encrypted, secondWrite), "Repeated writes must use fresh nonces.");
        expectThrows(SecurityException.class, () -> combined.read("invalid"));
        expectThrows(SecurityException.class, () -> combined.write(new byte[0], "invalid"));
        check(Arrays.equals(secondWrite, storage.read(token)), "Rejected writes must preserve the stored data.");
        secondWrite[secondWrite.length - 1] ^= 1;
        storage.write(secondWrite, token);
        expectThrows(SecurityException.class, () -> combined.read(token));
    }

    private static void testMeteringSession() throws IOException, SQLException {
        var directory = Files.createTempDirectory("smartcity360-test-");
        var database = directory.resolve("meter.db");
        try {
            var session = new MeteringSession(database);
            String token = MeteringSession.DEMO_TOKEN;
            var events = new ArrayList<MeteringEvent>();
            expectThrows(SecurityException.class, () -> session.write("M-101", "99", "wrong", events::add));
            check(events.equals(List.of(new MeteringEvent(TOKEN_CHECK, false))),
                    "Invalid authentication must stop the write before any transformations.");
            expectThrows(IllegalArgumentException.class, () -> session.write("M-101", "-5", token));
            events.clear();
            var first = session.write("M-101", "42.7", token, events::add);
            check(events.equals(List.of(new MeteringEvent(TOKEN_CHECK, true),
                    new MeteringEvent(COMPRESS, true), new MeteringEvent(ENCRYPT, true))),
                    "Write events must follow the real decorator order.");
            var second = session.write("M-101", "43.1", token);

            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 var statement = connection.createStatement()) {
                statement.execute("ALTER TABLE meter_readings DROP COLUMN token_hash");
                statement.execute("ALTER TABLE meter_readings DROP COLUMN protected_token");
            }
            var reopened = new MeteringSession(database);
            check(reopened.listReadings().equals(List.of(first, second)), "The schema update preserves earlier rows.");
            expectThrows(SecurityException.class, () -> reopened.read(first.id(), "wrong"));
            events.clear();
            check(reopened.read(first.id(), token, events::add).equals(new MeteringSession.Reading("M-101", "42.7")),
                    "The original key must decrypt the first reading after reopening.");
            check(events.equals(List.of(new MeteringEvent(TOKEN_CHECK, true),
                    new MeteringEvent(DECRYPT, true), new MeteringEvent(DECOMPRESS, true))),
                    "Read events must follow the actual reverse transformation order.");
            check(reopened.read(second.id(), token).kwh().equals("43.1"), "Read must use the selected ID.");

            var discarded = reopened.generateSample();
            var sample = reopened.generateSample();
            check(!sample.token().equals(discarded.token()), "Samples receive distinct tokens.");
            expectThrows(SecurityException.class, () -> reopened.write(sample.meterId(), sample.kwh(), discarded.token()));
            var generated = reopened.write(sample.meterId(), sample.kwh(), sample.token());
            expectThrows(SecurityException.class, () -> reopened.write(sample.meterId(), sample.kwh(), sample.token()));
            var finalSession = new MeteringSession(database);
            check(finalSession.listReadings().equals(List.of(first, second, generated)), "Only successful writes add rows.");
            expectThrows(SecurityException.class, () -> finalSession.read(generated.id(), token));
            check(finalSession.read(generated.id(), sample.token()).equals(
                    new MeteringSession.Reading(sample.meterId(), sample.kwh())), "Generated tokens work after reopening.");
            check(finalSession.readAutomatically(generated.id()).orElseThrow().equals(
                    new MeteringSession.Reading(sample.meterId(), sample.kwh())), "A protected token works after reopening.");
            check(finalSession.readAutomatically(first.id()).orElseThrow().kwh().equals("42.7"),
                    "Old demo-token rows open automatically.");

            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 var statement = connection.createStatement();
                 var row = statement.executeQuery(
                         "SELECT encrypted_payload, token_hash, protected_token FROM meter_readings ORDER BY reading_id")) {
                check(row.next() && row.getBytes(1).length >= 28, "A nonce and authentication tag must be stored.");
                check(!new String(row.getBytes(1), StandardCharsets.UTF_8).contains("M-101\n42.7"),
                        "SQLite must contain the encrypted payload.");
                check(row.getBytes(2) == null, "Demo-token rows have no per-reading token hash.");
                check(row.next() && row.next() && row.getBytes(2).length == 32,
                        "Generated-token rows store a SHA-256 verifier.");
                check(row.getBytes(3) != null && !new String(row.getBytes(3), StandardCharsets.UTF_8).contains(sample.token()),
                        "The remembered token must be protected.");
            }
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 var statement = connection.createStatement()) {
                statement.execute("ALTER TABLE meter_readings DROP COLUMN protected_token");
            }
            var migrated = new MeteringSession(database);
            check(migrated.readAutomatically(generated.id()).isEmpty(), "An older random token must be supplied once.");
            expectThrows(SecurityException.class, () -> migrated.read(generated.id(), token));
            check(migrated.readAutomatically(generated.id()).isEmpty(), "A failed read must not remember the wrong token.");
            migrated.read(generated.id(), sample.token());
            check(new MeteringSession(database).readAutomatically(generated.id()).orElseThrow().kwh().equals(sample.kwh()),
                    "A valid manual read enables future automatic reads.");
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 var statement = connection.prepareStatement(
                         "UPDATE meter_readings SET encrypted_payload = X'00' WHERE reading_id = ?")) {
                statement.setLong(1, first.id());
                statement.executeUpdate();
            }
            events.clear();
            expectThrows(SecurityException.class, () -> migrated.read(first.id(), token, events::add));
            check(events.equals(List.of(new MeteringEvent(TOKEN_CHECK, true), new MeteringEvent(DECRYPT, false))),
                    "A failed decryption must not report decompression.");

            migrated.clearReadings();
            var cleared = new MeteringSession(database);
            check(cleared.listReadings().isEmpty(), "Clearing readings must persist after reopening.");
            var savedAgain = cleared.write("M-101", "42.7", token);
            check(cleared.readAutomatically(savedAgain.id()).orElseThrow().kwh().equals("42.7"),
                    "Clearing history must leave the meter ready to save new readings.");
        } finally {
            Files.deleteIfExists(database);
            Files.deleteIfExists(directory);
        }
    }

    private static void testZones() {
        var city = new ZoneGroup("City", CITY);
        var north = new ZoneGroup("North", DISTRICT);
        var south = new ZoneGroup("South", DISTRICT);
        var northStreet = new ZoneGroup("North Street", STREET);
        var southStreet = new ZoneGroup("South Street", STREET);
        CityComponent library = new Building("Library", 10);
        CityComponent clinic = new Building("Clinic", 15);
        northStreet.add(library);
        southStreet.add(clinic);
        north.add(northStreet);
        south.add(southStreet);
        city.add(north);
        city.add(south);

        check(city.reportUsage() == 25 && north.reportUsage() == library.reportUsage(), "Usage rolls up from leaves.");
        north.shutdownPower();
        check(library.poweredBuildingCount() == 0 && clinic.poweredBuildingCount() == 1, "Shutdown stays in its subtree.");
        check(city.poweredBuildingCount() == 1 && city.reportUsage() == 25, "Historical readings survive shutdown.");
        expectThrows(IllegalArgumentException.class, () -> city.add(city));
        expectThrows(IllegalArgumentException.class, () -> city.add(library));
        expectThrows(IllegalArgumentException.class, () -> southStreet.add(library));
        northStreet.remove(library);
        southStreet.add(library);
        check(north.reportUsage() == 0 && south.reportUsage() == 25, "Moving a leaf updates each branch.");
        city.restorePower();
        check(city.poweredBuildingCount() == 2, "Restore power reaches each leaf.");
    }

    private static void testComplaints() {
        var manager = new ComplaintHandler("Manager", 10, null);
        var supervisor = new ComplaintHandler("Supervisor", 7, manager);
        var local = new ComplaintHandler("Local", 3, supervisor);
        String[] owners = {"Local", "Local", "Local", "Supervisor", "Supervisor", "Supervisor",
                "Supervisor", "Manager", "Manager", "Manager"};
        for (int severity = 1; severity <= owners.length; severity++) {
            var complaint = new Complaint("C-1", "Power issue", severity);
            check(local.handle(complaint).assignedTo().orElseThrow().equals(owners[severity - 1]),
                    "Unexpected owner for severity " + severity);
        }
        var outage = new Complaint("C-2", "District outage", 10);
        check(local.handle(outage).trace().equals(List.of("Local forwarded", "Supervisor forwarded", "Manager accepted")),
                "Escalation must report the actual path.");
        var shortRoute = new ComplaintHandler("Local", 3, null);
        check(shortRoute.handle(outage).assignedTo().isEmpty(), "An exhausted chain must leave the request unassigned.");
        check(local.handle(new Complaint("C-3", "Broken light", 1)).trace().size() == 1, "Assignment stops the chain.");
        expectThrows(IllegalArgumentException.class, () -> new Complaint("C-4", "Broken light", 0));
        expectThrows(IllegalArgumentException.class, () -> new Complaint("C-5", "Broken light", 11));
    }

    private static void testLighting() {
        var factory = new LightConfigurationFactory();
        var white = factory.get("LED", 50, "White");
        var first = new StreetLight("L-1", "Market Street", white);
        var second = new StreetLight("L-2", "Lake Street", factory.get("LED", 50, "White"));
        check(first.configuration() == second.configuration(), "Equal configurations must share object identity.");
        check(white != factory.get("LED", 50, "Warm white"), "Color must be part of the cache key.");
        check(white != factory.get("LED", 80, "White"), "Wattage must be part of the cache key.");
        check(white != factory.get("Halogen", 50, "White"), "Bulb type must be part of the cache key.");
        check(factory.configurationCount() == 4, "Cache size should equal the number of unique configurations.");
        first.switchOff();
        check(!first.isOn() && second.isOn(), "Per-light state must stay independent.");
        first.switchOn();
        check(first.isOn() && second.isOn(), "Restoring one light must preserve the other light.");

        var cityLights = SampleStreetlights.generate();
        var usage = SampleStreetlights.configurationUsage(cityLights);
        check(cityLights.size() == 10_000 && cityLights.get(0).configuration() == cityLights.get(3).configuration(),
                "The preview lamps must share a configuration in the full sample.");
        check(usage.stream().map(SampleStreetlights.ConfigurationUsage::lightCount).toList()
                .equals(List.of(3_334, 3_333, 3_333)), "Counts must reflect each shared object in the sample.");
    }

    private static void testPolicies() {
        var parser = new PolicyParser();
        var nightNoise = parser.parse("noiseLevel > 55 AND (hour >= 22 OR hour < 6)");
        check(nightNoise.interpret(new PolicyContext(68, 23)), "Night noise violation before midnight.");
        check(nightNoise.interpret(new PolicyContext(68, 2)), "Night noise violation after midnight.");
        check(!nightNoise.interpret(new PolicyContext(68, 12)), "Noise rule excludes midday.");
        check(!nightNoise.interpret(new PolicyContext(55, 23)), "Noise limit is exclusive.");
        var parking = parser.parse("hour>=8 AND hour<18");
        check(parking.interpret(new PolicyContext(40, 8)), "Parking starts at 08:00.");
        check(!parking.interpret(new PolicyContext(40, 18)), "Parking ends at 18:00.");
        check(parser.parse("hour == 8 OR hour == 9 AND NOT (noiseLevel <= 40)")
                .interpret(new PolicyContext(40, 8)), "NOT and AND must bind more tightly than OR.");
        check(!parser.parse("NOT hour == 8 AND hour == 9").interpret(new PolicyContext(40, 8)),
                "NOT binds to its immediate expression.");
        for (String invalid : List.of("wind > 5", "hour >=", "(hour < 6", "hour < 6 extra", "hour < NaN")) {
            expectThrows(IllegalArgumentException.class, () -> parser.parse(invalid));
        }
        expectThrows(IllegalArgumentException.class, () -> new PolicyContext(40, 24));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectThrows(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Expected " + type.getSimpleName(), error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
