package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import io.quarkus.maven.dependency.ArtifactKey;
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
        return collectDependencies(DependencyFlags.DEPLOYMENT_CP);
    }

    @Override
    public Collection<ResolvedDependency> getRuntimeDependencies() {
        return collectDependencies(DependencyFlags.RUNTIME_CP);
    }

    @Override
    public Iterable<ResolvedDependency> getDependencies(int flags) {
        return new FlagDependencyIterator(new int[] { flags });
    }

    public Iterable<ResolvedDependency> getDependenciesWithAnyFlag(int... flags) {
        return new FlagDependencyIterator(flags);
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
        return collectKeys(DependencyFlags.DEPLOYMENT_CP | DependencyFlags.CLASSLOADER_PARENT_FIRST);
    }

    @Override
    public Set<ArtifactKey> getRunnerParentFirst() {
        return collectKeys(DependencyFlags.DEPLOYMENT_CP | DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST);
    }

    @Override
    public Set<ArtifactKey> getLowerPriorityArtifacts() {
        return collectKeys(DependencyFlags.DEPLOYMENT_CP | DependencyFlags.CLASSLOADER_LESSER_PRIORITY);
    }

    @Override
    public Set<ArtifactKey> getReloadableWorkspaceDependencies() {
        return localProjectArtifacts;
    }

    @Override
    public Map<ArtifactKey, Set<String>> getRemovedResources() {
        return excludedResources;
    }

    private Collection<ResolvedDependency> collectDependencies(int flags) {
        var result = new ArrayList<ResolvedDependency>();
        for (var d : getDependencies(flags)) {
            result.add(d);
        }
        return result;
    }

    private Set<ArtifactKey> collectKeys(int flags) {
        var keys = new HashSet<ArtifactKey>();
        for (var d : getDependencies(flags)) {
            keys.add(d.getKey());
        }
        return keys;
    }

    private class FlagDependencyIterator implements Iterable<ResolvedDependency> {

        private final int[] flags;

        private FlagDependencyIterator(int[] flags) {
            this.flags = flags;
        }

        @Override
        public Iterator<ResolvedDependency> iterator() {
            return new Iterator<>() {

                final Iterator<ResolvedDependency> i = dependencies.iterator();
                ResolvedDependency next;

                {
                    moveOn();
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public ResolvedDependency next() {
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    var current = next;
                    moveOn();
                    return current;
                }

                private void moveOn() {
                    next = null;
                    while (i.hasNext()) {
                        var d = i.next();
                        if (d.hasAnyFlag(flags)) {
                            next = d;
                            break;
                        }
                    }
                }
            };
        }
    }
}
