package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchOrmElasticsearchMapperContext;

public final class HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem extends MultiBuildItem {

    public final HibernateSearchOrmElasticsearchMapperContext mapperContext;
    private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;

    public HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(
            HibernateSearchOrmElasticsearchMapperContext mapperContext,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig) {
        this.mapperContext = mapperContext;
        this.buildTimeConfig = buildTimeConfig;
    }

    public String getPersistenceUnitName() {
        return mapperContext.persistenceUnitName;
    }

    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getBuildTimeConfig() {
        return buildTimeConfig;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + getPersistenceUnitName() + "]";
    }
}
