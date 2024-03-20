package io.quarkus.resteasy.reactive.jackson.deployment.test;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Pond {

    @SecureField(rolesAllowed = "admin")
    private WaterQuality waterQuality;

    private String name;

    public WaterQuality getWaterQuality() {
        return waterQuality;
    }

    public void setWaterQuality(WaterQuality waterQuality) {
        this.waterQuality = waterQuality;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum WaterQuality {
        CLEAR,
        DIRTY
    }

}
