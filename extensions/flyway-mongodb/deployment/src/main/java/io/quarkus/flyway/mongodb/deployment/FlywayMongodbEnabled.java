package io.quarkus.flyway.mongodb.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.flyway.mongodb.runtime.FlywayMongodbBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Flyway-MongoDB extension is enabled.
 */
public class FlywayMongodbEnabled implements BooleanSupplier {

    private final FlywayMongodbBuildTimeConfig config;

    FlywayMongodbEnabled(FlywayMongodbBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
