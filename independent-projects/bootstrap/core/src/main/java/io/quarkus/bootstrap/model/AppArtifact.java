package io.quarkus.bootstrap.model;

import java.nio.file.Path;

/**
 * Represents an application (or its dependency) artifact.
 *
 * @author Alexey Loubyansky
 */
public class AppArtifact extends AppArtifactCoords {

    protected Path path;

    public AppArtifact(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isResolved() {
        return path != null;
    }
}
