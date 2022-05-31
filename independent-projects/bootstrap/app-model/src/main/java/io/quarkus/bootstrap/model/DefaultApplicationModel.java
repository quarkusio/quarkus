package io.quarkus.bootstrap.model;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultApplicationModel implements ApplicationModel, Serializable {

    private static final long serialVersionUID = -3878782344578748234L;

    private final ResolvedDependency appArtifact;
    private final List<ResolvedDependency> dependencies;
    private final PlatformImports platformImports;
    private final List<ExtensionCapabilities> capabilityContracts;
    private final Set<ArtifactKey> parentFirstArtifacts;
    private final Set<ArtifactKey> runnerParentFirstArtifacts;
    private final Set<ArtifactKey> lesserPriorityArtifacts;
    private final Set<ArtifactKey> localProjectArtifacts;
    private final Map<ArtifactKey, Set<String>> excludedResources;

    public DefaultApplicationModel(ApplicationModelBuilder builder) {
        this.appArtifact = builder.appArtifact;
        this.dependencies = builder.filter(builder.dependencies.values());
        this.platformImports = builder.platformImports;
        this.capabilityContracts = builder.extensionCapabilities;
        this.parentFirstArtifacts = builder.parentFirstArtifacts;
        this.runnerParentFirstArtifacts = builder.runnerParentFirstArtifacts;
        this.lesserPriorityArtifacts = builder.lesserPriorityArtifacts;
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
        return parentFirstArtifacts;
    }

    @Override
    public Set<ArtifactKey> getRunnerParentFirst() {
        return runnerParentFirstArtifacts;
    }

    @Override
    public Set<ArtifactKey> getLowerPriorityArtifacts() {
        return lesserPriorityArtifacts;
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
