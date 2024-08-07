package io.quarkus.smallrye.health.deployment;

import java.util.function.BooleanSupplier;

public class SmallRyeHealthActive implements BooleanSupplier {

    private final HealthBuildTimeConfig config;

    SmallRyeHealthActive(HealthBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled;
    }

}
