package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class RemovedResources {

    private List<RemovedResource> removedResources = new ArrayList<>(0);

    public RemovedResource artifact(String name) {
        RemovedResource removedResource = new RemovedResource(name);
        removedResources.add(removedResource);
        return removedResource;
    }

    public List<RemovedResource> getRemovedResources() {
        return removedResources;
    }

}
