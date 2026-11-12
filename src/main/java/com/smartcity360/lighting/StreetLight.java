package com.smartcity360.lighting;

import java.util.Objects;

public final class StreetLight {
    private final String id;
    private final String location;
    private final LightConfiguration configuration;
    private boolean on = true;

    public StreetLight(String id, String location, LightConfiguration configuration) {
        this.id = Objects.requireNonNull(id);
        this.location = Objects.requireNonNull(location);
        this.configuration = Objects.requireNonNull(configuration);
    }

    public String id() {
        return id;
    }

    public String location() {
        return location;
    }

    public LightConfiguration configuration() {
        return configuration;
    }

    public boolean isOn() {
        return on;
    }

    public void switchOff() {
        on = false;
    }

    public void switchOn() {
        on = true;
    }
}
