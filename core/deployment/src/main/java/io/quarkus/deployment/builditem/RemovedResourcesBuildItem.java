package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Aggregated view of all resources to be removed from dependencies.
 * <p>
 * This build item collects all {@link RemovedResourceBuildItem} instances and config-based
 * removed resources into a single map keyed by artifact.
 */
public final class RemovedResourcesBuildItem extends SimpleBuildItem {

    private final Map<ArtifactKey, Set<String>> removedResources;

    public RemovedResourcesBuildItem(Map<ArtifactKey, Set<String>> removedResources) {
        this.removedResources = Collections.unmodifiableMap(removedResources);
    }

    public Map<ArtifactKey, Set<String>> getRemovedResources() {
        return removedResources;
    }

    public boolean isEmpty() {
        return removedResources.isEmpty();
    }
}
