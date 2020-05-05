/**
 *
 */
package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenArtifactResolver {

    public static class Builder extends BootstrapMavenContextConfig<Builder> {

        private boolean reTryFailedResolutionsAgainstDefaultLocalRepo;

        private Builder() {
            super();
        }

        /**
         * In case custom local repository location is configured using {@link #setRepoHome(Path)},
         * this method can be used to enable artifact resolutions that failed for the configured
         * custom local repository to be re-tried against the default user local repository before
         * failing.
         * <p>
         * NOTE: the default behavior is <b>not</b> to use the default user local repository as the fallback one.
         *
         * @param value true if the failed resolution requests should be re-tried against the default
         *        user local repo before failing
         *
         * @return this builder instance
         */
        public Builder setReTryFailedResolutionsAgainstDefaultLocalRepo(boolean value) {
            this.reTryFailedResolutionsAgainstDefaultLocalRepo = value;
            return this;
        }

        public Builder setRepoHome(Path home) {
            if (home != null) {
                setLocalRepository(home.toString());
            }
            return this;
        }

        public MavenArtifactResolver build() throws AppModelResolverException {
            return new MavenArtifactResolver(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected final RepositorySystem repoSystem;
    protected final RepositorySystemSession repoSession;
    protected final List<RemoteRepository> remoteRepos;
    protected final MavenLocalRepositoryManager localRepoManager;
    protected final RemoteRepositoryManager remoteRepoManager;

    private MavenArtifactResolver(Builder builder) throws AppModelResolverException {
        final BootstrapMavenContext mvnSettings = new BootstrapMavenContext(builder);
        this.repoSystem = mvnSettings.getRepositorySystem();

        final RepositorySystemSession session = mvnSettings.getRepositorySystemSession();
        if (builder.localRepo != null && builder.reTryFailedResolutionsAgainstDefaultLocalRepo) {
            localRepoManager = new MavenLocalRepositoryManager(
                    repoSystem.newLocalRepositoryManager(session, new LocalRepository(builder.localRepo)),
                    Paths.get(BootstrapMavenContext.resolveLocalRepo(mvnSettings.getEffectiveSettings())));
            this.repoSession = new DefaultRepositorySystemSession(session).setLocalRepositoryManager(localRepoManager);
        } else {
            this.repoSession = session;
            localRepoManager = null;
        }

        this.remoteRepos = mvnSettings.getRemoteRepositories();
        this.remoteRepoManager = mvnSettings.getRemoteRepositoryManager();
    }

    public MavenArtifactResolver(BootstrapMavenContext mvnSettings) throws AppModelResolverException {
        this.repoSystem = mvnSettings.getRepositorySystem();
        this.repoSession = mvnSettings.getRepositorySystemSession();
        localRepoManager = null;
        this.remoteRepos = mvnSettings.getRemoteRepositories();
        this.remoteRepoManager = mvnSettings.getRemoteRepositoryManager();
    }

    public MavenLocalRepositoryManager getLocalRepositoryManager() {
        return localRepoManager;
    }

    public RepositorySystem getSystem() {
        return repoSystem;
    }

    public RepositorySystemSession getSession() {
        return repoSession;
    }

    public List<RemoteRepository> getRepositories() {
        return remoteRepos;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        remoteRepos.addAll(repos);
    }

    public ArtifactResult resolve(Artifact artifact) throws AppModelResolverException {
        return resolveInternal(artifact, remoteRepos);
    }

    public ArtifactResult resolve(Artifact artifact, List<RemoteRepository> mainRepos) throws AppModelResolverException {
        return resolveInternal(artifact, aggregateRepositories(mainRepos, remoteRepos));
    }

    private ArtifactResult resolveInternal(Artifact artifact, List<RemoteRepository> aggregatedRepos)
            throws AppModelResolverException {
        try {
            return repoSystem.resolveArtifact(repoSession,
                    new ArtifactRequest()
                            .setArtifact(artifact)
                            .setRepositories(aggregatedRepos));
        } catch (ArtifactResolutionException e) {
            throw new AppModelResolverException("Failed to resolve artifact " + artifact, e);
        }
    }

    public List<ArtifactResult> resolve(List<ArtifactRequest> artifacts) throws AppModelResolverException {
        try {
            return repoSystem.resolveArtifacts(repoSession, artifacts);
        } catch (ArtifactResolutionException e) {
            throw new AppModelResolverException("Failed to resolve artifacts", e);
        }
    }

    public ArtifactDescriptorResult resolveDescriptor(final Artifact artifact)
            throws AppModelResolverException {
        return resolveDescriptorInternal(artifact, remoteRepos);
    }

    public ArtifactDescriptorResult resolveDescriptor(final Artifact artifact, List<RemoteRepository> mainRepos)
            throws AppModelResolverException {
        return resolveDescriptorInternal(artifact, aggregateRepositories(mainRepos, remoteRepos));
    }

    private ArtifactDescriptorResult resolveDescriptorInternal(final Artifact artifact, List<RemoteRepository> aggregatedRepos)
            throws AppModelResolverException {
        try {
            return repoSystem.readArtifactDescriptor(repoSession,
                    new ArtifactDescriptorRequest()
                            .setArtifact(artifact)
                            .setRepositories(
                                    aggregatedRepos));
        } catch (ArtifactDescriptorException e) {
            throw new AppModelResolverException("Failed to read descriptor of " + artifact, e);
        }
    }

    public VersionRangeResult resolveVersionRange(Artifact artifact) throws AppModelResolverException {
        try {
            return repoSystem.resolveVersionRange(repoSession,
                    new VersionRangeRequest()
                            .setArtifact(artifact)
                            .setRepositories(remoteRepos));
        } catch (VersionRangeResolutionException ex) {
            throw new AppModelResolverException("Failed to resolve version range for " + artifact, ex);
        }
    }

    public CollectResult collectDependencies(Artifact artifact, List<Dependency> deps) throws AppModelResolverException {
        return collectDependencies(artifact, deps, Collections.emptyList());
    }

    public CollectResult collectDependencies(Artifact artifact, List<Dependency> deps, List<RemoteRepository> mainRepos)
            throws AppModelResolverException {
        final CollectRequest request = newCollectRequest(artifact, mainRepos);
        request.setDependencies(deps);
        try {
            return repoSystem.collectDependencies(repoSession, request);
        } catch (DependencyCollectionException e) {
            throw new AppModelResolverException("Failed to collect dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolveDependencies(Artifact artifact, List<Dependency> deps) throws AppModelResolverException {
        return resolveDependencies(artifact, deps, Collections.emptyList());
    }

    public DependencyResult resolveDependencies(Artifact artifact, List<Dependency> deps, List<RemoteRepository> mainRepos)
            throws AppModelResolverException {
        final CollectRequest request = newCollectRequest(artifact, mainRepos);
        request.setDependencies(deps);
        try {
            return repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest().setCollectRequest(request));
        } catch (DependencyResolutionException e) {
            throw new AppModelResolverException("Failed to resolve dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolveManagedDependencies(Artifact artifact, List<Dependency> deps, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, String... excludedScopes) throws AppModelResolverException {
        try {
            return repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest().setCollectRequest(
                            newCollectManagedRequest(artifact, deps, managedDeps, mainRepos, excludedScopes)));
        } catch (DependencyResolutionException e) {
            throw new AppModelResolverException("Failed to resolve dependencies for " + artifact, e);
        }
    }

    /**
     * Turns the list of dependencies into a simple dependency tree
     */
    public DependencyResult toDependencyTree(List<Dependency> deps, List<RemoteRepository> mainRepos)
            throws AppModelResolverException {
        DependencyResult result = new DependencyResult(
                new DependencyRequest().setCollectRequest(new CollectRequest(deps, Collections.emptyList(), mainRepos)));
        DefaultDependencyNode root = new DefaultDependencyNode((Dependency) null);
        result.setRoot(root);
        GenericVersionScheme vs = new GenericVersionScheme();
        for (Dependency i : deps) {
            DefaultDependencyNode node = new DefaultDependencyNode(i);
            try {
                node.setVersionConstraint(vs.parseVersionConstraint(i.getArtifact().getVersion()));
                node.setVersion(vs.parseVersion(i.getArtifact().getVersion()));
            } catch (InvalidVersionSpecificationException e) {
                throw new RuntimeException(e);
            }
            root.getChildren().add(node);
        }
        return result;
    }

    public CollectResult collectManagedDependencies(Artifact artifact, List<Dependency> deps, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, String... excludedScopes) throws AppModelResolverException {
        try {
            return repoSystem.collectDependencies(repoSession,
                    newCollectManagedRequest(artifact, deps, managedDeps, mainRepos, excludedScopes));
        } catch (DependencyCollectionException e) {
            throw new AppModelResolverException("Failed to collect dependencies for " + artifact, e);
        }
    }

    private CollectRequest newCollectManagedRequest(Artifact artifact, List<Dependency> deps, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, String... excludedScopes) throws AppModelResolverException {
        final List<RemoteRepository> aggregatedRepos = aggregateRepositories(mainRepos, remoteRepos);
        final ArtifactDescriptorResult descr = resolveDescriptorInternal(artifact, aggregatedRepos);
        Collection<String> excluded;
        if (excludedScopes.length == 0) {
            excluded = Collections.emptyList();
        } else if (excludedScopes.length == 1) {
            excluded = Collections.singleton(excludedScopes[0]);
        } else {
            excluded = Arrays.asList(excludedScopes);
            if (excludedScopes.length > 3) {
                excluded = new HashSet<>(Arrays.asList(excludedScopes));
            }
        }
        final List<Dependency> originalDeps = new ArrayList<>(descr.getDependencies().size());
        for (Dependency dep : descr.getDependencies()) {
            if (excluded.contains(dep.getScope())) {
                continue;
            }
            originalDeps.add(dep);
        }

        final List<Dependency> mergedManagedDeps = new ArrayList<Dependency>(
                managedDeps.size() + descr.getManagedDependencies().size());
        Map<AppArtifactKey, String> managedVersions = Collections.emptyMap();
        if (!managedDeps.isEmpty()) {
            managedVersions = new HashMap<>(managedDeps.size());
            for (Dependency dep : managedDeps) {
                managedVersions.put(getId(dep.getArtifact()), dep.getArtifact().getVersion());
                mergedManagedDeps.add(dep);
            }
        }
        if (!descr.getManagedDependencies().isEmpty()) {
            for (Dependency dep : descr.getManagedDependencies()) {
                final AppArtifactKey key = getId(dep.getArtifact());
                if (!managedVersions.containsKey(key)) {
                    mergedManagedDeps.add(dep);
                }
            }
        }

        return new CollectRequest()
                .setRootArtifact(artifact)
                .setDependencies(mergeDeps(deps, originalDeps, managedVersions))
                .setManagedDependencies(mergedManagedDeps)
                .setRepositories(aggregateRepositories(aggregatedRepos, newResolutionRepositories(descr.getRepositories())));
    }

    public List<RemoteRepository> newResolutionRepositories(List<RemoteRepository> repos) {
        return repos.isEmpty() ? Collections.emptyList() : repoSystem.newResolutionRepositories(repoSession, repos);
    }

    public List<RemoteRepository> aggregateRepositories(List<RemoteRepository> dominant, List<RemoteRepository> recessive) {
        return dominant.isEmpty() ? recessive
                : remoteRepoManager.aggregateRepositories(repoSession, dominant, recessive, false);
    }

    public void install(Artifact artifact) throws AppModelResolverException {
        try {
            repoSystem.install(repoSession, new InstallRequest().addArtifact(artifact));
        } catch (InstallationException ex) {
            throw new AppModelResolverException("Failed to install " + artifact, ex);
        }
    }

    private CollectRequest newCollectRequest(Artifact artifact, List<RemoteRepository> mainRepos)
            throws AppModelResolverException {
        return new CollectRequest()
                .setRoot(new Dependency(artifact, JavaScopes.RUNTIME))
                .setRepositories(aggregateRepositories(mainRepos, remoteRepos));
    }

    private List<Dependency> mergeDeps(List<Dependency> dominant, List<Dependency> recessive,
            Map<AppArtifactKey, String> managedVersions) {
        final int initialCapacity = dominant.size() + recessive.size();
        if (initialCapacity == 0) {
            return Collections.emptyList();
        }
        final List<Dependency> result = new ArrayList<Dependency>(initialCapacity);
        final Set<AppArtifactKey> ids = new HashSet<AppArtifactKey>(initialCapacity, 1.0f);
        for (Dependency dependency : dominant) {
            final AppArtifactKey id = getId(dependency.getArtifact());
            ids.add(id);
            final String managedVersion = managedVersions.get(id);
            if (managedVersion != null) {
                dependency = dependency.setArtifact(dependency.getArtifact().setVersion(managedVersion));
            }
            result.add(dependency);
        }
        for (Dependency dependency : recessive) {
            final AppArtifactKey id = getId(dependency.getArtifact());
            if (!ids.contains(id)) {
                final String managedVersion = managedVersions.get(id);
                if (managedVersion != null) {
                    dependency = dependency.setArtifact(dependency.getArtifact().setVersion(managedVersion));
                }
                result.add(dependency);
            }
        }
        return result;
    }

    private static AppArtifactKey getId(Artifact a) {
        return new AppArtifactKey(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension());
    }
}
