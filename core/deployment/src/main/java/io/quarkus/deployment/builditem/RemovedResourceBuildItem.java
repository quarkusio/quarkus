package io.quarkus.deployment.builditem;

import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.builder.item.MultiBuildItem;

public final class RemovedResourceBuildItem extends MultiBuildItem {

    private final AppArtifactKey artifact;
    private final Set<String> resources;

    public RemovedResourceBuildItem(AppArtifactKey artifact, Set<String> resources) {
        this.artifact = artifact;
        this.resources = resources;
    }

    public AppArtifactKey getArtifact() {
        return artifact;
    }

    public Set<String> getResources() {
        return resources;
    }
}
