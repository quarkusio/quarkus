package io.quarkus.test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ExtensionCapabilities;
import io.quarkus.bootstrap.model.ExtensionDevModeConfig;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * {@link ApplicationModel} implementation that allows overriding the application artifact
 * of another {@link ApplicationModel} instance.
 */
class DevModeTestApplicationModel implements ApplicationModel {

    private final ResolvedDependency appArtifact;
    private final ApplicationModel delegate;

    DevModeTestApplicationModel(ResolvedDependency testAppArtifact, ApplicationModel delegate) {
        this.appArtifact = testAppArtifact;
        this.delegate = delegate;
    }

    @Override
    public ResolvedDependency getAppArtifact() {
        return appArtifact;
    }

    @Override
    public Collection<ResolvedDependency> getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public Iterable<ResolvedDependency> getDependencies(int flags) {
        return delegate.getDependencies(flags);
    }

    @Override
    public Iterable<ResolvedDependency> getDependenciesWithAnyFlag(int flags) {
        return delegate.getDependenciesWithAnyFlag(flags);
    }

    @Override
    public Collection<ResolvedDependency> getRuntimeDependencies() {
        return delegate.getRuntimeDependencies();
    }

    @Override
    public PlatformImports getPlatforms() {
        return delegate.getPlatforms();
    }

    @Override
    public Collection<ExtensionCapabilities> getExtensionCapabilities() {
        return delegate.getExtensionCapabilities();
    }

    @Override
    public Set<ArtifactKey> getParentFirst() {
        return delegate.getParentFirst();
    }

    @Override
    public Set<ArtifactKey> getRunnerParentFirst() {
        return delegate.getRunnerParentFirst();
    }

    @Override
    public Set<ArtifactKey> getLowerPriorityArtifacts() {
        return delegate.getLowerPriorityArtifacts();
    }

    @Override
    public Set<ArtifactKey> getReloadableWorkspaceDependencies() {
        return delegate.getReloadableWorkspaceDependencies();
    }

    @Override
    public Map<ArtifactKey, Set<String>> getRemovedResources() {
        return delegate.getRemovedResources();
    }

    @Override
    public Collection<ExtensionDevModeConfig> getExtensionDevModeConfig() {
        return delegate.getExtensionDevModeConfig();
    }
}