package io.quarkus.smallrye.health.deployment;

import java.util.function.BooleanSupplier;

public class SmallRyeHealthActive implements BooleanSupplier {

    private final SmallRyeHealthBuildTimeConfig config;

    SmallRyeHealthActive(SmallRyeHealthBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
