package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

public class DefaultApplicationModel implements ApplicationModel, Serializable {

    private static final long serialVersionUID = -3878782344578748234L;

    private final ResolvedDependency appArtifact;
    private final List<ResolvedDependency> dependencies;
    private final PlatformImports platformImports;
    private final List<ExtensionCapabilities> capabilityContracts;
    private final Set<ArtifactKey> localProjectArtifacts;
    private final Map<ArtifactKey, Set<String>> excludedResources;

    public DefaultApplicationModel(ApplicationModelBuilder builder) {
        this.appArtifact = builder.appArtifact;
        this.dependencies = builder.buildDependencies();
        this.platformImports = builder.platformImports;
        this.capabilityContracts = builder.extensionCapabilities;
        this.localProjectArtifacts = builder.reloadableWorkspaceModules;
        this.excludedResources = builder.excludedResources;
    }

    @Override
    public ResolvedDependency getAppArtifact() {
        return appArtifact;
    }

    @Override
    public Collection<ResolvedDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public PlatformImports getPlatforms() {
        return platformImports;
    }

    @Override
    public Collection<ExtensionCapabilities> getExtensionCapabilities() {
        return capabilityContracts;
    }

    @Override
    public Set<ArtifactKey> getParentFirst() {
        return getDependencies().stream().filter(Dependency::isClassLoaderParentFirst).map(Dependency::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ArtifactKey> getRunnerParentFirst() {
        return getDependencies().stream().filter(d -> d.isFlagSet(DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST))
                .map(Dependency::getKey).collect(Collectors.toSet());
    }

    @Override
    public Set<ArtifactKey> getLowerPriorityArtifacts() {
        return getDependencies().stream().filter(d -> d.isFlagSet(DependencyFlags.CLASSLOADER_LESSER_PRIORITY))
                .map(Dependency::getKey).collect(Collectors.toSet());
    }

    @Override
    public Set<ArtifactKey> getReloadableWorkspaceDependencies() {
        return localProjectArtifacts;
    }

    @Override
    public Map<ArtifactKey, Set<String>> getRemovedResources() {
        return excludedResources;
    }
}
