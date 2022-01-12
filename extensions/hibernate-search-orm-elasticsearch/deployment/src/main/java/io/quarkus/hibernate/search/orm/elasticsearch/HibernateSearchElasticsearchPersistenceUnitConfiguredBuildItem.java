package io.quarkus.hibernate.search.orm.elasticsearch;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;

public final class HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;
    private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;

    public HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(String persistenceUnitName,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
        this.buildTimeConfig = buildTimeConfig;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getBuildTimeConfig() {
        return buildTimeConfig;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + persistenceUnitName + "]";
    }
}
