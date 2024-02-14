package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneBuildTimeConfig;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.management.HibernateSearchStandaloneManagementConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Search extension and its management is enabled.
 */
public class HibernateSearchStandaloneManagementEnabled extends HibernateSearchStandaloneEnabled {

    private final HibernateSearchStandaloneManagementConfig config;

    HibernateSearchStandaloneManagementEnabled(HibernateSearchStandaloneBuildTimeConfig config,
            HibernateSearchStandaloneManagementConfig managementConfig) {
        super(config);
        this.config = managementConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && config.enabled();
    }

}
