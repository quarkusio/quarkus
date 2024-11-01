package io.quarkus.deployment.builditem;

import java.nio.file.Path;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

/**
 * Represents resources to be removed from a dependency when packaging the application.
 */
public final class RemovedResourceBuildItem extends MultiBuildItem {

    private final Path artifactPath;
    private final ArtifactKey artifact;
    private final Set<String> resources;

    public RemovedResourceBuildItem(ArtifactKey artifact, Set<String> resources) {
        this.artifact = artifact;
        this.resources = resources;
        this.artifactPath = null;
    }

    /**
     * @deprecated In favor of {@link #RemovedResourceBuildItem(ArtifactKey, Set)}
     * @param artifact artifact key
     * @param resources resources to be removed from the application
     */
    @Deprecated(forRemoval = true)
    public RemovedResourceBuildItem(GACT artifact, Set<String> resources) {
        this.artifactPath = null;
        this.artifact = artifact;
        this.resources = resources;
    }

    public RemovedResourceBuildItem(Path artifactPath, Set<String> resources) {
        this.artifact = null;
        this.resources = resources;
        this.artifactPath = artifactPath;
    }

    public ArtifactKey getArtifact() {
        return artifact;
    }

    public Set<String> getResources() {
        return resources;
    }

    public Path getArtifactPath() {
        return this.artifactPath;
    }

    public boolean hasArtifact() {
        return this.artifact != null;
    }
}
