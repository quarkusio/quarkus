package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares dependency resources removed by a Quarkus extension.
 * Configure this block through {@code quarkusExtension.removedResources}.
 */
public class RemovedResources {

    private List<RemovedResource> removedResources = new ArrayList<>(0);

    /**
     * Adds a removal declaration for an artifact.
     *
     * @param name the artifact key accepted by Quarkus, such as {@code group-id:artifact-id}
     * @return the new artifact removal declaration
     */
    public RemovedResource artifact(String name) {
        RemovedResource removedResource = new RemovedResource(name);
        removedResources.add(removedResource);
        return removedResource;
    }

    /**
     * Returns artifact removal declarations in declaration order.
     *
     * @return the mutable removal declarations
     */
    public List<RemovedResource> getRemovedResources() {
        return removedResources;
    }

}
