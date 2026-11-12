package com.smartcity360.stream;

public final class BasicMeterStream implements MeterStream {
    private byte[] stored = new byte[0];

    @Override public void write(byte[] data, String token) { stored = data.clone(); }
    @Override public byte[] read(String token) { return stored.clone(); }
    public int storedBytes() { return stored.length; }
}
