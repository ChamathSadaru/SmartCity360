package com.smartcity360.stream;

import java.util.function.Consumer;

public record MeteringEvent(Step step, boolean succeeded) {
    static final Consumer<MeteringEvent> IGNORE = event -> {};

    public enum Step {
        TOKEN_CHECK, COMPRESS, ENCRYPT, DECRYPT, DECOMPRESS
    }
}
