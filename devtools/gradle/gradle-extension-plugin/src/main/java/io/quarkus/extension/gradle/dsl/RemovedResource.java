package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class RemovedResource {

    private String artifact;
    private List<String> removedResources = new ArrayList<>(1);

    public RemovedResource(String artifact) {
        this.artifact = artifact;
    }

    public RemovedResource resource(String resource) {
        removedResources.add(resource);
        return this;
    }

    public String getArtifactName() {
        return artifact;
    }

    public List<String> getRemovedResources() {
        return removedResources;
    }

}
