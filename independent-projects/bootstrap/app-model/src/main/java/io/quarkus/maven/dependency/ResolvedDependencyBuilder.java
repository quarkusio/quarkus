package io.quarkus.maven.dependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

public class ResolvedDependencyBuilder extends AbstractDependencyBuilder<ResolvedDependencyBuilder, ResolvedDependency>
        implements ResolvedDependency {

    public static ResolvedDependencyBuilder newInstance() {
        return new ResolvedDependencyBuilder();
    }

    PathCollection resolvedPaths;
    WorkspaceModule workspaceModule;
    private volatile ArtifactCoords coords;
    private Collection<ArtifactCoords> deps = Set.of();

    @Override
    public PathCollection getResolvedPaths() {
        return resolvedPaths;
    }

    public ResolvedDependencyBuilder setResolvedPath(Path path) {
        this.resolvedPaths = path == null ? null : PathList.of(path);
        return this;
    }

    public ResolvedDependencyBuilder setResolvedPaths(PathCollection resolvedPaths) {
        this.resolvedPaths = resolvedPaths;
        return this;
    }

    @Override
    public WorkspaceModule getWorkspaceModule() {
        return workspaceModule;
    }

    public ResolvedDependencyBuilder setWorkspaceModule(WorkspaceModule projectModule) {
        this.workspaceModule = projectModule;
        if (projectModule != null) {
            setWorkspaceModule();
        }
        return this;
    }

    public ArtifactCoords getArtifactCoords() {
        return coords == null ? coords = ArtifactCoords.of(groupId, artifactId, classifier, type, version) : coords;
    }

    public ResolvedDependencyBuilder addDependency(ArtifactCoords coords) {
        if (coords != null) {
            if (deps.isEmpty()) {
                deps = new HashSet<>();
            }
            deps.add(coords);
        }
        return this;
    }

    public ResolvedDependencyBuilder addDependencies(Collection<ArtifactCoords> deps) {
        if (!deps.isEmpty()) {
            if (this.deps.isEmpty()) {
                this.deps = new HashSet<>(deps);
            } else {
                this.deps.addAll(deps);
            }
        }
        return this;
    }

    public ResolvedDependencyBuilder setDependencies(Collection<ArtifactCoords> deps) {
        this.deps = deps;
        return this;
    }

    @Override
    public Collection<ArtifactCoords> getDependencies() {
        return deps;
    }

    @Override
    public ResolvedDependency build() {
        return new ResolvedArtifactDependency(this);
    }
}
