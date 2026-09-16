package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares resources to remove from one dependency artifact while building an application.
 * <p>
 * Instances are created through {@link RemovedResources#artifact(String)}. Resource names are accumulated in
 * declaration order and written to the extension descriptor for the selected artifact.
 */
public class RemovedResource {

    private String artifact;
    private List<String> removedResources = new ArrayList<>(1);

    /**
     * Creates a removal declaration for an artifact key.
     *
     * @param artifact the artifact key accepted by Quarkus, such as {@code group-id:artifact-id}
     */
    public RemovedResource(String artifact) {
        this.artifact = artifact;
    }

    /**
     * Adds one resource path to remove.
     *
     * @param resource the resource path
     * @return this declaration
     */
    public RemovedResource resource(String resource) {
        removedResources.add(resource);
        return this;
    }

    /**
     * Returns the target artifact key.
     *
     * @return the artifact key
     */
    public String getArtifactName() {
        return artifact;
    }

    /**
     * Returns resource paths in declaration order.
     *
     * @return the mutable resource-path list
     */
    public List<String> getRemovedResources() {
        return removedResources;
    }

}
