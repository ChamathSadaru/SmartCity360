package com.smartcity360.stream;

public interface MeterStream {
    void write(byte[] data, String token);
    byte[] read(String token);
}
