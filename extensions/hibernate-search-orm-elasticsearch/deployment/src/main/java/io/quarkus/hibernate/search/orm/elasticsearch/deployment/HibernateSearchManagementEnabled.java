package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Search extension and its management is enabled.
 */
public class HibernateSearchManagementEnabled extends HibernateSearchEnabled {

    HibernateSearchManagementEnabled(HibernateSearchElasticsearchBuildTimeConfig config) {
        super(config);
    }

    @Override
    public boolean getAsBoolean() {
        return super.getAsBoolean() && config.management().enabled();
    }

}
