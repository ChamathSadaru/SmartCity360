package com.smartcity360.stream;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class MeteringSession {
    public static final String DEMO_TOKEN = "city-demo";

    public record ReadingRow(long id, String meterId, Instant recordedAt) {}
    public record Reading(String meterId, String kwh) {}
    public record SavedReading(ReadingRow row, Reading reading) {}
    public record Sample(String meterId, String kwh, String token) {}

    private static final byte[] DEMO_TOKEN_HASH = AuthenticationDecorator.tokenHash(DEMO_TOKEN);
    private final SqliteMeterStore store;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();
    private byte[] sampleTokenHash;

    public MeteringSession(Path database) {
        store = new SqliteMeterStore(database);
        key = store.loadKey();
    }

    public List<ReadingRow> listReadings() {
        return store.list();
    }

    public Sample generateSample() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sampleTokenHash = AuthenticationDecorator.tokenHash(token);
        return new Sample("M-" + random.nextInt(1000, 10000),
                BigDecimal.valueOf(random.nextInt(100, 10000), 1).toPlainString(), token);
    }

    public void clearSampleToken() {
        sampleTokenHash = null;
    }

    public ReadingRow write(String meterId, String kwh, String token) {
        return write(meterId, kwh, token, MeteringEvent.IGNORE);
    }

    public ReadingRow write(String meterId, String kwh, String token, Consumer<MeteringEvent> observer) {
        return save(meterId, kwh, token, observer).row();
    }

    public SavedReading save(String meterId, String kwh, String token, Consumer<MeteringEvent> observer) {
        String id = meterId.strip();
        if (!id.matches("[A-Za-z0-9_-]{1,24}")) {
            throw new IllegalArgumentException("Enter a meter ID with up to 24 letters, digits, hyphens or underscores.");
        }
        String value = kwh.strip();
        if (!value.matches("[0-9]{1,9}(\\.[0-9]{1,3})?")) {
            throw new IllegalArgumentException("Enter a non-negative reading with up to three decimal places.");
        }

        var buffer = new BasicMeterStream();
        var reading = new Reading(id, new BigDecimal(value).toPlainString());
        String payload = id + "\n" + reading.kwh();
        boolean sample = sampleTokenHash != null && token != null
                && MessageDigest.isEqual(sampleTokenHash, AuthenticationDecorator.tokenHash(token));
        byte[] tokenHash = sample ? sampleTokenHash : null;
        pipeline(buffer, tokenHash, observer).write(payload.getBytes(StandardCharsets.UTF_8), token);
        var entry = store.insert(id, Instant.now(), buffer.read(null), tokenHash, token);
        clearSampleToken();
        return new SavedReading(entry, reading);
    }

    public void clearReadings() {
        store.clearReadings();
    }

    public Reading read(long id, String token) {
        return read(id, token, MeteringEvent.IGNORE);
    }

    public Reading read(long id, String token, Consumer<MeteringEvent> observer) {
        var stored = store.loadReading(id);
        var reading = readPayload(stored, token, observer);
        if (stored.tokenHash() != null && stored.protectedToken() == null) {
            store.rememberToken(id, token);
        }
        return reading;
    }

    public Optional<Reading> readAutomatically(long id) {
        return readAutomatically(id, MeteringEvent.IGNORE);
    }

    public Optional<Reading> readAutomatically(long id, Consumer<MeteringEvent> observer) {
        var stored = store.loadReading(id);
        if (stored.tokenHash() == null) {
            return Optional.of(readPayload(stored, DEMO_TOKEN, observer));
        }
        if (stored.protectedToken() == null) {
            return Optional.empty();
        }
        return Optional.of(readPayload(stored, SqliteMeterStore.recoverToken(stored.protectedToken()), observer));
    }

    private Reading readPayload(SqliteMeterStore.StoredReading stored, String token, Consumer<MeteringEvent> observer) {
        var buffer = new BasicMeterStream();
        buffer.write(stored.payload(), null);
        String payload = new String(pipeline(buffer, stored.tokenHash(), observer).read(token), StandardCharsets.UTF_8);
        int separator = payload.indexOf('\n');
        return new Reading(payload.substring(0, separator), payload.substring(separator + 1));
    }

    private MeterStream pipeline(MeterStream storage, byte[] tokenHash, Consumer<MeteringEvent> observer) {
        return new AuthenticationDecorator(
                new CompressionDecorator(new EncryptionDecorator(storage, key, observer), observer),
                tokenHash == null ? DEMO_TOKEN_HASH : tokenHash, observer);
    }
}
