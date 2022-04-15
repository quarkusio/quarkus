package io.quarkus.maven.dependency;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import io.quarkus.paths.PathTree;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

public class ResolvedArtifactDependency extends ArtifactDependency implements ResolvableDependency, Serializable {

    private static final long serialVersionUID = 4038042391733012566L;

    private PathCollection paths;
    private WorkspaceModule module;
    private volatile transient PathTree contentTree;

    public ResolvedArtifactDependency(ArtifactCoords coords) {
        this(coords, (PathCollection) null);
    }

    public ResolvedArtifactDependency(ArtifactCoords coords, Path resolvedPath) {
        this(coords, PathList.of(resolvedPath));
    }

    public ResolvedArtifactDependency(String groupId, String artifactId, String classifier, String type, String version,
            Path resolvedPath) {
        this(groupId, artifactId, classifier, type, version, PathList.of(resolvedPath));
    }

    public ResolvedArtifactDependency(String groupId, String artifactId, String classifier, String type, String version,
            PathCollection resolvedPath) {
        super(groupId, artifactId, classifier, type, version);
        this.paths = resolvedPath;
    }

    public ResolvedArtifactDependency(ArtifactCoords coords, PathCollection resolvedPaths) {
        super(coords);
        this.paths = resolvedPaths;
    }

    public ResolvedArtifactDependency(ResolvedDependencyBuilder builder) {
        super(builder);
        this.paths = builder.getResolvedPaths();
        this.module = builder.getWorkspaceModule();
    }

    @Override
    public PathCollection getResolvedPaths() {
        return paths;
    }

    public void setResolvedPaths(PathCollection paths) {
        this.paths = paths;
    }

    @Override
    public WorkspaceModule getWorkspaceModule() {
        return module;
    }

    @Override
    public PathTree getContentTree() {
        return contentTree == null ? contentTree = ResolvableDependency.super.getContentTree() : contentTree;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(module, paths);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ResolvableDependency))
            return false;
        ResolvableDependency other = (ResolvableDependency) obj;
        return Objects.equals(module, other.getWorkspaceModule()) && Objects.equals(paths, other.getResolvedPaths());
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(toGACTVString()).append(paths);
        if (module != null) {
            buf.append(" " + module);
        }
        return buf.toString();
    }
}
