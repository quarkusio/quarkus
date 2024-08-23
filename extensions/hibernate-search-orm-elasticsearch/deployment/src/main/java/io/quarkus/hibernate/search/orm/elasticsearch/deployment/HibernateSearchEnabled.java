package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Search extension is enabled.
 */
public class HibernateSearchEnabled implements BooleanSupplier {

    protected final HibernateSearchElasticsearchBuildTimeConfig config;

    HibernateSearchEnabled(HibernateSearchElasticsearchBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
