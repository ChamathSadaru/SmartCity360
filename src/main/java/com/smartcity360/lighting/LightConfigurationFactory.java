package com.smartcity360.lighting;

import java.util.HashMap;
import java.util.Map;

public final class LightConfigurationFactory {
    private final Map<LightConfiguration, LightConfiguration> configurations = new HashMap<>();

    public LightConfiguration get(String bulbType, int watts, String color) {
        var key = new LightConfiguration(bulbType, watts, color);
        return configurations.computeIfAbsent(key, configuration -> configuration);
    }

    public int configurationCount() {
        return configurations.size();
    }
}
