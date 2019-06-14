package io.quarkus.deployment.index;

import java.nio.file.Path;

public class ArtifactIndex {

    private final ArtifactResolver resolver;

    public ArtifactIndex(ArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public Path getPath(String groupId, String artifactId, String classifier) {
        ResolvedArtifact artifact = resolver.getArtifact(groupId, artifactId, classifier);
        if (artifact == null) {
            throw new RuntimeException("Unable to resolve artifact " + groupId + ":" + artifactId + ":" + classifier);
        }
        return artifact.getArtifactPath();
    }
}
