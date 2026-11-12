package com.smartcity360.stream;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Consumer;

import static com.smartcity360.stream.MeteringEvent.Step.TOKEN_CHECK;

public final class AuthenticationDecorator extends StreamDecorator {
    private final byte[] expectedHash;

    public AuthenticationDecorator(MeterStream inner, String expectedToken) {
        this(inner, tokenHash(Objects.requireNonNull(expectedToken)), MeteringEvent.IGNORE);
    }

    AuthenticationDecorator(MeterStream inner, byte[] expectedHash, Consumer<MeteringEvent> observer) {
        super(inner, observer);
        this.expectedHash = expectedHash.clone();
    }

    static byte[] tokenHash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable.", error);
        }
    }

    private void check(String token) {
        boolean accepted = token != null && MessageDigest.isEqual(expectedHash, tokenHash(token));
        record(TOKEN_CHECK, accepted);
        if (!accepted) {
            throw new SecurityException("Access denied. Check the access token.");
        }
    }

    @Override public void write(byte[] data, String token) { check(token); inner.write(data, token); }
    @Override public byte[] read(String token) { check(token); return inner.read(token); }
}
