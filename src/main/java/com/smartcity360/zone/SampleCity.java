package com.smartcity360.zone;

import static com.smartcity360.zone.ZoneGroup.ZoneType.CITY;
import static com.smartcity360.zone.ZoneGroup.ZoneType.DISTRICT;
import static com.smartcity360.zone.ZoneGroup.ZoneType.STREET;

public final class SampleCity {
    private SampleCity() {}

    public static ZoneGroup create() {
        var city = new ZoneGroup("SmartCity360", CITY);
        var north = new ZoneGroup("North District", DISTRICT);
        var south = new ZoneGroup("South District", DISTRICT);
        var marketStreet = new ZoneGroup("Market Street", STREET);
        var lakeStreet = new ZoneGroup("Lake Street", STREET);
        marketStreet.add(new Building("Library", 42.5));
        marketStreet.add(new Building("Clinic", 27.5));
        lakeStreet.add(new Building("Community Centre", 30));
        north.add(marketStreet);
        south.add(lakeStreet);
        city.add(north);
        city.add(south);
        return city;
    }
}
