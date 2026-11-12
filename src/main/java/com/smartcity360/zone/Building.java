package com.smartcity360.zone;

public final class Building implements CityComponent {
    private final String name;
    private final double usageKWh;
    private boolean powerOn = true;
    private ZoneGroup parent;
    public Building(String name, double usageKWh) {
        if (name == null || name.isBlank() || !Double.isFinite(usageKWh) || usageKWh < 0)
            throw new IllegalArgumentException("A building needs a name and valid usage.");
        this.name = name; this.usageKWh = usageKWh;
    }
    @Override public String name() { return name; }
    void attachTo(ZoneGroup parent) {
        if (this.parent != null) throw new IllegalArgumentException("This building already belongs to a street.");
        this.parent = parent;
    }
    void detach() { parent = null; }
    @Override public double reportUsage() { return usageKWh; }
    @Override public void shutdownPower() { powerOn = false; }
    @Override public void restorePower() { powerOn = true; }
    @Override public int buildingCount() { return 1; }
    @Override public int poweredBuildingCount() { return powerOn ? 1 : 0; }
    @Override public String toString() { return name; }
}
