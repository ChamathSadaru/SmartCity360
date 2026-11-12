package com.smartcity360.zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ZoneGroup implements CityComponent {
    public enum ZoneType { CITY, DISTRICT, STREET }
    private final String name;
    private final ZoneType type;
    private final List<CityComponent> children = new ArrayList<>();
    private ZoneGroup parent;
    public ZoneGroup(String name, ZoneType type) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter a zone name.");
        this.name = name; this.type = Objects.requireNonNull(type);
    }
    public void add(CityComponent child) {
        Objects.requireNonNull(child);
        if (children.contains(child)) throw new IllegalArgumentException("This item is already in the zone.");
        boolean allowed = switch (type) {
            case CITY -> child instanceof ZoneGroup g && g.type == ZoneType.DISTRICT;
            case DISTRICT -> child instanceof ZoneGroup g && g.type == ZoneType.STREET;
            case STREET -> child instanceof Building;
        };
        if (!allowed) throw new IllegalArgumentException("Use city, district, street, then building order.");
        if (child instanceof ZoneGroup group) {
            if (group.parent != null) throw new IllegalArgumentException("This zone already has a parent.");
            group.parent = this;
        }
        if (child instanceof Building building) building.attachTo(this);
        children.add(child);
    }
    public void remove(CityComponent child) {
        if (children.remove(child)) {
            if (child instanceof ZoneGroup group) group.parent = null;
            if (child instanceof Building building) building.detach();
        }
    }
    public List<CityComponent> children() { return List.copyOf(children); }
    public ZoneType type() { return type; }
    @Override public String name() { return name; }
    @Override public double reportUsage() { return children.stream().mapToDouble(CityComponent::reportUsage).sum(); }
    @Override public void shutdownPower() { children.forEach(CityComponent::shutdownPower); }
    @Override public void restorePower() { children.forEach(CityComponent::restorePower); }
    @Override public int buildingCount() { return children.stream().mapToInt(CityComponent::buildingCount).sum(); }
    @Override public int poweredBuildingCount() { return children.stream().mapToInt(CityComponent::poweredBuildingCount).sum(); }
    @Override public String toString() { return name; }
}
