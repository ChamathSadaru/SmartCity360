package com.smartcity360.zone;

public interface CityComponent {
    String name();
    double reportUsage();
    void shutdownPower();
    void restorePower();
    int buildingCount();
    int poweredBuildingCount();
}
