package io.quarkus.bootstrap.model;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import java.io.Serializable;
import java.nio.file.Path;

/**
 * Represents an application (or its dependency) artifact.
 *
 * @author Alexey Loubyansky
 */
public class AppArtifact extends AppArtifactCoords implements ResolvedDependency, Serializable {

    protected PathsCollection paths;
    private final WorkspaceModule module;
    private final String scope;
    private final int flags;

    public AppArtifact(AppArtifactCoords coords) {
        this(coords, null);
    }

    public AppArtifact(AppArtifactCoords coords, WorkspaceModule module) {
        this(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType(), coords.getVersion(),
                module, "compile", 0);
    }

    public AppArtifact(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
        module = null;
        scope = "compile";
        flags = 0;
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
        module = null;
        scope = "compile";
        flags = 0;
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version,
            WorkspaceModule module, String scope, int flags) {
        super(groupId, artifactId, classifier, type, version);
        this.module = module;
        this.scope = scope;
        this.flags = flags;
    }

    /**
     * @deprecated in favor of {@link #getResolvedPaths()}
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
    @Override
    public boolean isResolved() {
        return paths != null && !paths.isEmpty();
    }

    @Override
    public PathCollection getResolvedPaths() {
        return paths == null ? null : PathList.from(paths);
    }

    @Override
    public WorkspaceModule getWorkspaceModule() {
        return module;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public int getFlags() {
        return flags;
    }
}
