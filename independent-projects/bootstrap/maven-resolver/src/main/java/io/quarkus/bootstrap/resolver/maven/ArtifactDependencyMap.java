package io.quarkus.bootstrap.resolver.maven;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.graph.Dependency;

import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * A record of direct dependencies of a given application dependency.
 */
class ArtifactDependencyMap {

    private final ApplicationDependencyMap appDeps;
    private final Dependency dep;
    private final Map<ArtifactKey, Dependency> deps = new ConcurrentHashMap<>();

    ArtifactDependencyMap(ApplicationDependencyMap appDeps, Dependency dep) {
        this.appDeps = Objects.requireNonNull(appDeps);
        this.dep = Objects.requireNonNull(dep);
    }

    Dependency getDependency() {
        return dep;
    }

    void putDependency(Dependency dep) {
        deps.put(DependencyUtils.getKey(dep.getArtifact()), dep);
    }

    ApplicationDependencyMap getApplicationDependencies() {
        return appDeps;
    }

    Set<ArtifactKey> getKeys() {
        return deps.keySet();
    }

    Collection<Dependency> getDependencies() {
        return deps.values();
    }
}
