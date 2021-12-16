package io.quarkus.deployment.builditem;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

public final class RemovedResourceBuildItem extends MultiBuildItem {

    private final GACT artifact;
    private final Set<String> resources;

    @Deprecated
    public RemovedResourceBuildItem(ArtifactKey artifact, Set<String> resources) {
        this.artifact = new GACT(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType());
        this.resources = resources;
    }

    public RemovedResourceBuildItem(GACT artifact, Set<String> resources) {
        this.artifact = artifact;
        this.resources = resources;
    }

    public ArtifactKey getArtifact() {
        return artifact;
    }

    public Set<String> getResources() {
        return resources;
    }
}
