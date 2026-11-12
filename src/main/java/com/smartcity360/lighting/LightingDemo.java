package com.smartcity360.lighting;

public final class LightingDemo {
    public static void main(String[] args) {
        var lights = SampleStreetlights.generate();
        var configurations = SampleStreetlights.configurationUsage(lights);
        StreetLight first = lights.get(0);
        StreetLight fourth = lights.get(3);
        first.switchOff();

        System.out.printf("%,d lights hold %,d distinct configuration references.%n",
                lights.size(), configurations.size());
        System.out.printf("One configuration per light would retain %,d configurations.%n", lights.size());
        System.out.printf("%s (%s) and %s (%s) share the same configuration object: %s%n",
                first.id(), first.location(), fourth.id(), fourth.location(),
                first.configuration() == fourth.configuration());
        System.out.printf("%s powered: %s | %s powered: %s%n",
                first.id(), first.isOn(), fourth.id(), fourth.isOn());
    }
}
