package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import org.hibernate.SessionFactory;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.Key;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchOrmElasticsearchMapperContext;

public final class HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem extends MultiBuildItem {

    public final HibernateSearchOrmElasticsearchMapperContext mapperContext;
    private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;
    @Key(SessionFactory.class)
    private final String persistenceUnitName;

    public HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(
            HibernateSearchOrmElasticsearchMapperContext mapperContext,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig) {
        this.mapperContext = mapperContext;
        this.buildTimeConfig = buildTimeConfig;
        this.persistenceUnitName = mapperContext.persistenceUnitName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getBuildTimeConfig() {
        return buildTimeConfig;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + getPersistenceUnitName() + "]";
    }
}
