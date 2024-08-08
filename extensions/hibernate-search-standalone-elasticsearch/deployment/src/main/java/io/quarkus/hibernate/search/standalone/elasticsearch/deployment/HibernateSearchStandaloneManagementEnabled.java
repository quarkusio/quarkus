package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Search extension and its management is enabled.
 */
public class HibernateSearchStandaloneManagementEnabled extends HibernateSearchStandaloneEnabled {

    HibernateSearchStandaloneManagementEnabled(HibernateSearchStandaloneBuildTimeConfig config) {
        super(config);
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && config.management().enabled();
    }

}
