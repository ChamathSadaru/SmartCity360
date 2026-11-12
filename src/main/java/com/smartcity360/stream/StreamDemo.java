package com.smartcity360.stream;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.KeyGenerator;

public final class StreamDemo {
    public static void main(String[] args) throws GeneralSecurityException {
        String token = "city-demo";
        byte[] readings = "M-101,42.7 kWh\n".repeat(64).getBytes(StandardCharsets.UTF_8);
        var storage = new BasicMeterStream();
        var compressed = new CompressionDecorator(storage);
        compressed.write(readings, token);
        int compressedBytes = storage.storedBytes();

        var keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        MeterStream pipeline = new AuthenticationDecorator(
                new CompressionDecorator(new EncryptionDecorator(storage, keyGenerator.generateKey())), token);
        pipeline.write(readings, token);
        byte[] restored = pipeline.read(token);

        System.out.println("Write path: token check -> GZIP -> AES-GCM -> storage");
        System.out.printf("Original: %d B | GZIP: %d B | Encrypted GZIP: %d B%n",
                readings.length, compressedBytes, storage.storedBytes());
        System.out.printf("First restored reading: %s%n",
                new String(restored, StandardCharsets.UTF_8).lines().findFirst().orElse("(empty)"));
        System.out.printf("Round trip matches: %s%n", Arrays.equals(readings, restored));

        try {
            pipeline.read("wrong-token");
        } catch (SecurityException error) {
            System.out.println(error.getMessage());
        }
    }
}
