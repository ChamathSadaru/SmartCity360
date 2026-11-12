package com.smartcity360.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.smartcity360.stream.MeteringEvent.Step.COMPRESS;
import static com.smartcity360.stream.MeteringEvent.Step.DECOMPRESS;

public final class CompressionDecorator extends StreamDecorator {
    public CompressionDecorator(MeterStream inner) { super(inner); }
    CompressionDecorator(MeterStream inner, Consumer<MeteringEvent> observer) { super(inner, observer); }

    @Override public void write(byte[] data, String token) {
        var output = new ByteArrayOutputStream();
        try {
            try (var gzip = new GZIPOutputStream(output)) {
                gzip.write(data);
            }
        } catch (IOException error) {
            record(COMPRESS, false);
            throw new IllegalStateException("Could not compress the reading.", error);
        }
        record(COMPRESS, true);
        inner.write(output.toByteArray(), token);
    }

    @Override public byte[] read(String token) {
        byte[] compressed = inner.read(token);
        byte[] restored;
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            restored = gzip.readAllBytes();
        } catch (IOException error) {
            record(DECOMPRESS, false);
            throw new IllegalStateException("Could not read compressed data.", error);
        }
        record(DECOMPRESS, true);
        return restored;
    }
}
