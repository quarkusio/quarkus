package io.quarkus.flyway.multitenant.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.flyway.multitenant.runtime.FlywayMultiTenantBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Flyway extension is enabled.
 */
public class FlywayMultiTenantEnabled implements BooleanSupplier {

    private final FlywayMultiTenantBuildTimeConfig config;

    FlywayMultiTenantEnabled(FlywayMultiTenantBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled;
    }

}
