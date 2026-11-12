package com.smartcity360.policy;

import java.util.Map;

public final class PolicyContext {
    private final Map<String, Double> facts;
    public PolicyContext(double noiseLevel, int hour) {
        if (!Double.isFinite(noiseLevel) || noiseLevel < 0 || noiseLevel > 200)
            throw new IllegalArgumentException("Noise level must be from 0 to 200 dB.");
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("Hour must be from 0 to 23.");
        facts = Map.of("noiseLevel", noiseLevel, "hour", (double) hour);
    }
    public double getFact(String name) {
        Double value = facts.get(name);
        if (value == null) throw new IllegalArgumentException("Unknown field: " + name);
        return value;
    }
}
