package com.smartcity360.stream;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class StreamDecorator implements MeterStream {
    protected final MeterStream inner;
    private final Consumer<MeteringEvent> observer;

    protected StreamDecorator(MeterStream inner) {
        this(inner, MeteringEvent.IGNORE);
    }

    StreamDecorator(MeterStream inner, Consumer<MeteringEvent> observer) {
        this.inner = Objects.requireNonNull(inner);
        this.observer = Objects.requireNonNull(observer);
    }

    final void record(MeteringEvent.Step step, boolean succeeded) {
        observer.accept(new MeteringEvent(step, succeeded));
    }
}
