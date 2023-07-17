package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;

public final class HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;
    private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;
    private final Set<String> backendNamesForIndexedEntities;
    private Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;

    public HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(String persistenceUnitName,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig,
            Set<String> backendNamesForIndexedEntities,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
        this.buildTimeConfig = buildTimeConfig;
        this.backendNamesForIndexedEntities = backendNamesForIndexedEntities;
        this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public Set<String> getBackendNamesForIndexedEntities() {
        return backendNamesForIndexedEntities;
    }

    public Map<String, Set<String>> getBackendAndIndexNamesForSearchExtensions() {
        return backendAndIndexNamesForSearchExtensions;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + persistenceUnitName + "]";
    }
}
