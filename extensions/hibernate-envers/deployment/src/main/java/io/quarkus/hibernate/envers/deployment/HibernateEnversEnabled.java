package io.quarkus.hibernate.envers.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Envers extension is enabled.
 */
public class HibernateEnversEnabled implements BooleanSupplier {

    private final HibernateEnversBuildTimeConfig config;

    HibernateEnversEnabled(HibernateEnversBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
