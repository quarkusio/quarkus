package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Represents an application (or its dependency) artifact.
 *
 * @author Alexey Loubyansky
 */
public class AppArtifact extends AppArtifactCoords implements Serializable {

    protected PathsCollection paths;

    public AppArtifact(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
    }

    /**
     * @deprecated in favor of {@link #getPaths()}
     */
    @Deprecated
    public Path getPath() {
        return paths.getSinglePath();
    }

    /**
     * Associates the artifact with the given path
     *
     * @param path artifact location
     */
    public void setPath(Path path) {
        setPaths(PathsCollection.of(path));
    }

    /**
     * Collection of the paths that collectively constitute the artifact's content.
     * Normally, especially in the Maven world, an artifact is resolved to a single path,
     * e.g. a JAR or a project's output directory. However, in Gradle, depending on the build/test phase,
     * artifact's content may need to be represented as a collection of paths.
     *
     * @return collection of paths that constitute the artifact's content
     */
    public PathsCollection getPaths() {
        return paths;
    }

    /**
     * Associates the artifact with a collection of paths that constitute its content.
     *
     * @param paths collection of paths that constitute the artifact's content.
     */
    public void setPaths(PathsCollection paths) {
        this.paths = paths;
    }

    /**
     * Whether the artifact has been resolved, i.e. associated with paths
     * that constitute its content.
     *
     * @return true if the artifact has been resolved, otherwise - false
     */
    public boolean isResolved() {
        return paths != null && !paths.isEmpty();
    }
}
