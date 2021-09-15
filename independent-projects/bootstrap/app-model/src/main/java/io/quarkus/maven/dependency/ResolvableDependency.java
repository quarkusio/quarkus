package io.quarkus.maven.dependency;

import io.quarkus.paths.PathCollection;

public interface ResolvableDependency extends ResolvedDependency {

    void setResolvedPaths(PathCollection paths);
}
