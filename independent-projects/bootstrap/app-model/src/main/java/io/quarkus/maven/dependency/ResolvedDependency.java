package io.quarkus.maven.dependency;

import java.nio.file.Path;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.MultiRootPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;

public interface ResolvedDependency extends Dependency {

    PathCollection getResolvedPaths();

    default boolean isResolved() {
        final PathCollection paths = getResolvedPaths();
        return paths != null && !paths.isEmpty();
    }

    default WorkspaceModule getWorkspaceModule() {
        return null;
    }

    default ArtifactSources getSources() {
        final WorkspaceModule module = getWorkspaceModule();
        return module == null ? null : module.getSources(getClassifier());
    }

    default PathTree getContentTree() {
        return getContentTree(null);
    }

    default PathTree getContentTree(PathFilter pathFilter) {
        final WorkspaceModule module = getWorkspaceModule();
        final PathTree workspaceTree = module == null ? EmptyPathTree.getInstance() : module.getContentTree(getClassifier());
        if (!workspaceTree.isEmpty()) {
            return workspaceTree;
        }
        final PathCollection paths = getResolvedPaths();
        if (paths == null || paths.isEmpty()) {
            return EmptyPathTree.getInstance();
        }
        if (paths.isSinglePath()) {
            final Path p = paths.getSinglePath();
            return isJar() ? PathTree.ofDirectoryOrArchive(p, pathFilter) : PathTree.ofDirectoryOrFile(p, pathFilter);
        }
        final PathTree[] trees = new PathTree[paths.size()];
        int i = 0;
        for (Path p : paths) {
            trees[i++] = PathTree.ofDirectoryOrArchive(p, pathFilter);
        }
        return new MultiRootPathTree(trees);
    }
}
