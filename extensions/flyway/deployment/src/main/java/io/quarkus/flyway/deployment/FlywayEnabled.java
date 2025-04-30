package io.quarkus.flyway.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Flyway extension is enabled.
 */
public class FlywayEnabled implements BooleanSupplier {

    private final FlywayBuildTimeConfig config;

    FlywayEnabled(FlywayBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
