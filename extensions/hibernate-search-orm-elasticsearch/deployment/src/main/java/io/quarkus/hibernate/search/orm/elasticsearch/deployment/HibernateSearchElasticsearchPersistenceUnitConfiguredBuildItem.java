package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;

public final class HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;
    private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;
    private final boolean defaultBackendIsUsed;
    private Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;

    public HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(String persistenceUnitName,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig, boolean defaultBackendIsUsed,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
        this.buildTimeConfig = buildTimeConfig;
        this.defaultBackendIsUsed = defaultBackendIsUsed;
        this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public boolean isDefaultBackendUsed() {
        return defaultBackendIsUsed;
    }

    public Map<String, Set<String>> getBackendAndIndexNamesForSearchExtensions() {
        return backendAndIndexNamesForSearchExtensions;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + persistenceUnitName + "]";
    }
}
