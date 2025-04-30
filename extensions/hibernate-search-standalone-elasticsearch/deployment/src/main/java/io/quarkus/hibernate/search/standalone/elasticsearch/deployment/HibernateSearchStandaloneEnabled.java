package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Search extension is enabled.
 */
public class HibernateSearchStandaloneEnabled implements BooleanSupplier {

    protected final HibernateSearchStandaloneBuildTimeConfig config;

    HibernateSearchStandaloneEnabled(HibernateSearchStandaloneBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
