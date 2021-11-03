package io.quarkus.maven.dependency;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import java.nio.file.Path;

public class ResolvedDependencyBuilder extends AbstractDependencyBuilder<ResolvedDependencyBuilder, ResolvedDependency> {

    public static ResolvedDependencyBuilder newInstance() {
        return new ResolvedDependencyBuilder();
    }

    PathCollection resolvedPaths;
    WorkspaceModule workspaceModule;

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

    public WorkspaceModule getWorkspaceModule() {
        return workspaceModule;
    }

    public ResolvedDependencyBuilder setWorkspaceModule(WorkspaceModule projectModule) {
        this.workspaceModule = projectModule;
        return this;
    }

    @Override
    public ResolvedDependency build() {
        return new ResolvedArtifactDependency(this);
    }
}
