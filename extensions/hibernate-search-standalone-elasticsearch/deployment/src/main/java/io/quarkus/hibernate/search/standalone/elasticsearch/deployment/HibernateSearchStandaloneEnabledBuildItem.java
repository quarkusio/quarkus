package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class HibernateSearchStandaloneEnabledBuildItem extends SimpleBuildItem {

    private final Set<String> backendNamesForIndexedEntities;
    private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;
    private final Set<String> rootAnnotationMappedClassNames;

    public HibernateSearchStandaloneEnabledBuildItem(Set<String> backendNamesForIndexedEntities,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
            Set<String> rootAnnotationMappedClassNames) {
        this.backendNamesForIndexedEntities = backendNamesForIndexedEntities;
        this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
        this.rootAnnotationMappedClassNames = rootAnnotationMappedClassNames;
    }

    public Set<String> getBackendNamesForIndexedEntities() {
        return backendNamesForIndexedEntities;
    }

    public Map<String, Set<String>> getBackendAndIndexNamesForSearchExtensions() {
        return backendAndIndexNamesForSearchExtensions;
    }

    public Set<String> getRootAnnotationMappedClassNames() {
        return rootAnnotationMappedClassNames;
    }
}
