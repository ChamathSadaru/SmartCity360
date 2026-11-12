package com.smartcity360.stream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.Consumer;

import static com.smartcity360.stream.MeteringEvent.Step.DECRYPT;
import static com.smartcity360.stream.MeteringEvent.Step.ENCRYPT;

public final class EncryptionDecorator extends StreamDecorator {
    private static final int NONCE_LENGTH = 12;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();
    public EncryptionDecorator(MeterStream inner, SecretKey key) {
        this(inner, key, MeteringEvent.IGNORE);
    }

    EncryptionDecorator(MeterStream inner, SecretKey key, Consumer<MeteringEvent> observer) {
        super(inner, observer);
        this.key = key;
    }

    @Override public void write(byte[] data, String token) {
        byte[] stored;
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(data);
            stored = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, stored, nonce.length, encrypted.length);
        } catch (GeneralSecurityException error) {
            record(ENCRYPT, false);
            throw new IllegalStateException("Could not encrypt the reading.", error);
        }
        record(ENCRYPT, true);
        inner.write(stored, token);
    }

    @Override public byte[] read(String token) {
        byte[] stored = inner.read(token);
        if (stored.length < NONCE_LENGTH + 16) {
            record(DECRYPT, false);
            throw new SecurityException("Encrypted data is incomplete.");
        }
        byte[] decrypted;
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, stored, 0, NONCE_LENGTH));
            decrypted = cipher.doFinal(stored, NONCE_LENGTH, stored.length - NONCE_LENGTH);
        } catch (GeneralSecurityException error) {
            record(DECRYPT, false);
            throw new SecurityException("Encrypted data failed its integrity check.", error);
        }
        record(DECRYPT, true);
        return decrypted;
    }
}
