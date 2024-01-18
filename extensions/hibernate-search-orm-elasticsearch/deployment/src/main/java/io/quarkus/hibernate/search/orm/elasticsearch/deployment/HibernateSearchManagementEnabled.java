package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.management.HibernateSearchManagementConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Search extension and its management is enabled.
 */
public class HibernateSearchManagementEnabled extends HibernateSearchEnabled {

    private final HibernateSearchManagementConfig config;

    HibernateSearchManagementEnabled(HibernateSearchElasticsearchBuildTimeConfig config,
            HibernateSearchManagementConfig managementConfig) {
        super(config);
        this.config = managementConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && config.enabled();
    }

}
