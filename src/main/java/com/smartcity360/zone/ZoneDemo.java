package com.smartcity360.zone;

import java.util.Locale;

public final class ZoneDemo {
    public static void main(String[] args) {
        var city = SampleCity.create();
        var north = city.children().get(0);

        System.out.println("Before shutdown");
        report(city, "");
        north.shutdownPower();
        System.out.println("After North District shutdown");
        report(city, "");
    }

    private static void report(CityComponent node, String indent) {
        System.out.printf(Locale.ROOT, "%s%s: %.1f kWh recorded, %d/%d buildings powered%n",
                indent, node.name(), node.reportUsage(), node.poweredBuildingCount(), node.buildingCount());
        if (node instanceof ZoneGroup zone) {
            for (CityComponent child : zone.children()) {
                report(child, indent + "  ");
            }
        }
    }
}
