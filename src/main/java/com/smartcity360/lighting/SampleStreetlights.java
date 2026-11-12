package com.smartcity360.lighting;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public final class SampleStreetlights {
    private static final int LIGHT_COUNT = 10_000;

    public record ConfigurationUsage(LightConfiguration configuration, int lightCount) {}

    private SampleStreetlights() {}

    public static List<StreetLight> generate() {
        var factory = new LightConfigurationFactory();
        var lights = new ArrayList<StreetLight>(LIGHT_COUNT);
        for (int index = 0; index < LIGHT_COUNT; index++) {
            LightConfiguration configuration = switch (index % 3) {
                case 0 -> factory.get("LED", 50, "White");
                case 1 -> factory.get("LED", 65, "Warm white");
                default -> factory.get("LED", 80, "White");
            };
            int number = index + 1;
            lights.add(new StreetLight("L-" + number, "Pole " + number, configuration));
        }
        return List.copyOf(lights);
    }

    public static List<ConfigurationUsage> configurationUsage(List<StreetLight> lights) {
        var counts = new IdentityHashMap<LightConfiguration, Integer>();
        var ordered = new ArrayList<LightConfiguration>();
        for (StreetLight light : lights) {
            var configuration = light.configuration();
            if (counts.merge(configuration, 1, Integer::sum) == 1) {
                ordered.add(configuration);
            }
        }
        return ordered.stream()
                .map(configuration -> new ConfigurationUsage(configuration, counts.get(configuration)))
                .toList();
    }
}
