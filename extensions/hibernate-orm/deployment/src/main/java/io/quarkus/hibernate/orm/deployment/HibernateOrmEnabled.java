package io.quarkus.hibernate.orm.deployment;

import java.util.function.BooleanSupplier;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate ORM extension is enabled.
 */
public class HibernateOrmEnabled implements BooleanSupplier {

    private final HibernateOrmConfig config;

    HibernateOrmEnabled(HibernateOrmConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
