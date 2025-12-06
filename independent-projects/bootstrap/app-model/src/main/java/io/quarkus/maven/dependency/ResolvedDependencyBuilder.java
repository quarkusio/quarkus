package io.quarkus.maven.dependency;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.Mappable;
import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

public class ResolvedDependencyBuilder extends AbstractDependencyBuilder<ResolvedDependencyBuilder, ResolvedDependency>
        implements ResolvedDependency {

    static void putInMap(ResolvedDependency dependency, Map<String, Object> map, MappableCollectionFactory factory) {
        ArtifactDependency.putInMap(dependency, map, factory);
        map.put(BootstrapConstants.MAPPABLE_RESOLVED_PATHS,
                Mappable.iterableToStringCollection(dependency.getResolvedPaths(), factory));

        final Collection<ArtifactCoords> deps = dependency.getDependencies();
        if (!deps.isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_DEPENDENCIES,
                    Mappable.toStringCollection(deps, ArtifactCoords::toGACTVString, factory));
        }

        if (dependency.getWorkspaceModule() != null) {
            map.put(BootstrapConstants.MAPPABLE_MODULE, dependency.getWorkspaceModule().asMap(factory));
        }
    }

    public static ResolvedDependencyBuilder newInstance() {
        return new ResolvedDependencyBuilder();
    }

    PathCollection resolvedPaths;
    WorkspaceModule workspaceModule;
    private volatile ArtifactCoords coords;
    private Collection<ArtifactCoords> deps = Set.of();

    @Override
    public ResolvedDependencyBuilder fromMap(Map<String, Object> map) {
        super.fromMap(map);

        Collection<String> resolvedPathsStr = (Collection<String>) map.get(BootstrapConstants.MAPPABLE_RESOLVED_PATHS);
        final Path[] pathArr = new Path[resolvedPathsStr.size()];
        int i = 0;
        for (var pathStr : resolvedPathsStr) {
            pathArr[i++] = Path.of(pathStr);
        }
        setResolvedPaths(PathList.of(pathArr));

        Collection<String> depsStr = (Collection<String>) map.get(BootstrapConstants.MAPPABLE_DEPENDENCIES);
        if (depsStr != null) {
            final List<ArtifactCoords> deps = new ArrayList<>(depsStr.size());
            for (String depStr : depsStr) {
                deps.add(ArtifactCoords.fromString(depStr));
            }
            setDependencies(deps);
        }

        Map<String, Object> moduleMap = (Map<String, Object>) map.get(BootstrapConstants.MAPPABLE_MODULE);
        if (moduleMap != null) {
            setWorkspaceModule(WorkspaceModule.builder().fromMap(moduleMap).build());
        }
        return this;
    }

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

    @Override
    public Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(7);
        putInMap(this, map, factory);
        return map;
    }
}
