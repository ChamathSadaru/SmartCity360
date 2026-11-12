package com.smartcity360.lighting;

public record LightConfiguration(String bulbType, int watts, String color) {
    public LightConfiguration {
        if (bulbType == null || bulbType.isBlank() || watts <= 0 || color == null || color.isBlank()) {
            throw new IllegalArgumentException("A light configuration needs a bulb type, positive wattage and color.");
        }
    }
}
