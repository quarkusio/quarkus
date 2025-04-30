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

    private static final long serialVersionUID = -5247678201356725379L;

    private final ResolvedDependency appArtifact;
    private final List<ResolvedDependency> dependencies;
    private final PlatformImports platformImports;
    private final List<ExtensionCapabilities> capabilityContracts;
    private final Set<ArtifactKey> localProjectArtifacts;
    private final Map<ArtifactKey, Set<String>> excludedResources;
    private final List<ExtensionDevModeConfig> extensionDevConfig;

    public DefaultApplicationModel(ApplicationModelBuilder builder) {
        this.appArtifact = builder.appArtifact.build();
        this.dependencies = builder.buildDependencies();
        this.platformImports = builder.platformImports;
        this.capabilityContracts = List.copyOf(builder.extensionCapabilities);
        this.localProjectArtifacts = Set.copyOf(builder.reloadableWorkspaceModules);
        this.excludedResources = Map.copyOf(builder.excludedResources);
        this.extensionDevConfig = List.copyOf(builder.extensionDevConfig);
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
        return new FlagDependencyIterator(flags, false);
    }

    @Override
    public Iterable<ResolvedDependency> getDependenciesWithAnyFlag(int flags) {
        return new FlagDependencyIterator(flags, true);
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

    @Override
    public Collection<ExtensionDevModeConfig> getExtensionDevModeConfig() {
        return extensionDevConfig;
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

        private final int flags;
        private final boolean any;

        /**
         * Iterates over application model dependencies that match requested flags.
         * The {@code any} boolean argument controls whether any or all the flags have to match
         * for a dependency to be selected.
         *
         * @param flags flags to match
         * @param any whether any or all of the flags have to be matched
         */
        private FlagDependencyIterator(int flags, boolean any) {
            this.flags = flags;
            this.any = any;
        }

        @Override
        public Iterator<ResolvedDependency> iterator() {
            return new Iterator<>() {

                int index = 0;
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
                    while (index < dependencies.size() && next == null) {
                        var d = dependencies.get(index++);
                        if (any) {
                            if (d.isAnyFlagSet(flags)) {
                                next = d;
                            }
                        } else if (d.isFlagSet(flags)) {
                            next = d;
                        }
                    }
                }
            };
        }
    }
}
