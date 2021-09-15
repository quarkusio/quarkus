/**
 *
 */
package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.workspace.ProjectModuleResolver;
import io.quarkus.bootstrap.util.PropertyUtils;
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
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
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
import org.eclipse.aether.version.Version;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenArtifactResolver {

    private static final String SECONDARY_LOCAL_REPO_PROP = "io.quarkus.maven.secondary-local-repo";

    public static class Builder extends BootstrapMavenContextConfig<Builder> {

        private Path secondaryLocalRepo;

        private Builder() {
            super();
        }

        public Builder setSecondaryLocalRepo(Path secondaryLocalRepo) {
            this.secondaryLocalRepo = secondaryLocalRepo;
            return this;
        }

        public MavenArtifactResolver build() throws BootstrapMavenException {
            return new MavenArtifactResolver(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected final BootstrapMavenContext context;
    protected final RepositorySystem repoSystem;
    protected final RepositorySystemSession repoSession;
    protected final List<RemoteRepository> remoteRepos;
    protected final MavenLocalRepositoryManager localRepoManager;
    protected final RemoteRepositoryManager remoteRepoManager;

    private MavenArtifactResolver(Builder builder) throws BootstrapMavenException {
        this.context = new BootstrapMavenContext(builder);
        this.repoSystem = context.getRepositorySystem();

        final RepositorySystemSession session = context.getRepositorySystemSession();
        final String secondaryRepo = PropertyUtils.getProperty(SECONDARY_LOCAL_REPO_PROP);
        if (secondaryRepo != null) {
            builder.secondaryLocalRepo = Paths.get(secondaryRepo);
        }
        if (builder.secondaryLocalRepo != null) {
            localRepoManager = new MavenLocalRepositoryManager(
                    session.getLocalRepositoryManager(),
                    builder.secondaryLocalRepo);
            this.repoSession = new DefaultRepositorySystemSession(session).setLocalRepositoryManager(localRepoManager);
        } else {
            this.repoSession = session;
            localRepoManager = null;
        }

        this.remoteRepos = context.getRemoteRepositories();
        this.remoteRepoManager = context.getRemoteRepositoryManager();
    }

    public MavenArtifactResolver(BootstrapMavenContext mvnSettings) throws BootstrapMavenException {
        this.context = mvnSettings;
        this.repoSystem = mvnSettings.getRepositorySystem();
        this.repoSession = mvnSettings.getRepositorySystemSession();
        localRepoManager = null;
        this.remoteRepos = mvnSettings.getRemoteRepositories();
        this.remoteRepoManager = mvnSettings.getRemoteRepositoryManager();
    }

    public ProjectModuleResolver getProjectModuleResolver() {
        return context.getWorkspace() == null ? null : context.getWorkspace();
    }

    public BootstrapMavenContext getMavenContext() {
        return context;
    }

    public RemoteRepositoryManager getRemoteRepositoryManager() {
        return remoteRepoManager;
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

    public ArtifactResult resolve(Artifact artifact) throws BootstrapMavenException {
        return resolveInternal(artifact, remoteRepos);
    }

    public ArtifactResult resolve(Artifact artifact, List<RemoteRepository> mainRepos) throws BootstrapMavenException {
        return resolveInternal(artifact, aggregateRepositories(mainRepos, remoteRepos));
    }

    private ArtifactResult resolveInternal(Artifact artifact, List<RemoteRepository> aggregatedRepos)
            throws BootstrapMavenException {
        try {
            return repoSystem.resolveArtifact(repoSession,
                    new ArtifactRequest()
                            .setArtifact(artifact)
                            .setRepositories(aggregatedRepos));
        } catch (ArtifactResolutionException e) {
            throw new BootstrapMavenException("Failed to resolve artifact " + artifact, e);
        }
    }

    public List<ArtifactResult> resolve(List<ArtifactRequest> artifacts) throws BootstrapMavenException {
        try {
            return repoSystem.resolveArtifacts(repoSession, artifacts);
        } catch (ArtifactResolutionException e) {
            throw new BootstrapMavenException("Failed to resolve artifacts", e);
        }
    }

    public ArtifactDescriptorResult resolveDescriptor(final Artifact artifact)
            throws BootstrapMavenException {
        return resolveDescriptorInternal(artifact, remoteRepos);
    }

    public ArtifactDescriptorResult resolveDescriptor(final Artifact artifact, List<RemoteRepository> mainRepos)
            throws BootstrapMavenException {
        return resolveDescriptorInternal(artifact, aggregateRepositories(mainRepos, remoteRepos));
    }

    private ArtifactDescriptorResult resolveDescriptorInternal(final Artifact artifact, List<RemoteRepository> aggregatedRepos)
            throws BootstrapMavenException {
        try {
            return repoSystem.readArtifactDescriptor(repoSession,
                    new ArtifactDescriptorRequest()
                            .setArtifact(artifact)
                            .setRepositories(
                                    aggregatedRepos));
        } catch (ArtifactDescriptorException e) {
            throw new BootstrapMavenException("Failed to read descriptor of " + artifact, e);
        }
    }

    public VersionRangeResult resolveVersionRange(Artifact artifact) throws BootstrapMavenException {
        try {
            return repoSystem.resolveVersionRange(repoSession,
                    new VersionRangeRequest()
                            .setArtifact(artifact)
                            .setRepositories(remoteRepos));
        } catch (VersionRangeResolutionException ex) {
            throw new BootstrapMavenException("Failed to resolve version range for " + artifact, ex);
        }
    }

    public String getLatestVersionFromRange(Artifact artifact, String range) throws BootstrapMavenException {
        return getLatest(resolveVersionRange(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), range)));
    }

    private String getLatest(final VersionRangeResult rangeResult) {
        final List<Version> versions = rangeResult.getVersions();
        if (versions.isEmpty()) {
            return null;
        }
        Version next = versions.get(0);
        for (int i = 1; i < versions.size(); ++i) {
            final Version candidate = versions.get(i);
            if (candidate.compareTo(next) > 0) {
                next = candidate;
            }
        }
        return next.toString();
    }

    public CollectResult collectDependencies(Artifact artifact, List<Dependency> deps) throws BootstrapMavenException {
        return collectDependencies(artifact, deps, Collections.emptyList());
    }

    public CollectResult collectDependencies(Artifact artifact, List<Dependency> deps, List<RemoteRepository> mainRepos)
            throws BootstrapMavenException {
        return collectDependencies(artifact, deps, mainRepos, Collections.emptyList());
    }

    public CollectResult collectDependencies(Artifact artifact, List<Dependency> deps, List<RemoteRepository> mainRepos,
            Collection<Exclusion> exclusions) throws BootstrapMavenException {
        final CollectRequest request = newCollectRequest(artifact, mainRepos, exclusions);
        request.setDependencies(deps);
        try {
            return repoSystem.collectDependencies(repoSession, request);
        } catch (DependencyCollectionException e) {
            throw new BootstrapMavenException("Failed to collect dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolveDependencies(Artifact artifact, List<Dependency> deps) throws BootstrapMavenException {
        return resolveDependencies(artifact, deps, Collections.emptyList());
    }

    public DependencyResult resolveDependencies(Artifact artifact, List<Dependency> deps, List<RemoteRepository> mainRepos)
            throws BootstrapMavenException {
        final CollectRequest request = newCollectRequest(artifact, mainRepos);
        request.setDependencies(deps);
        try {
            return repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest().setCollectRequest(request));
        } catch (DependencyResolutionException e) {
            throw new BootstrapMavenException("Failed to resolve dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolveManagedDependencies(Artifact artifact, List<Dependency> deps, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, String... excludedScopes) throws BootstrapMavenException {
        try {
            return repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest().setCollectRequest(
                            newCollectManagedRequest(artifact, deps, managedDeps, mainRepos, excludedScopes)));
        } catch (DependencyResolutionException e) {
            throw new BootstrapMavenException("Failed to resolve dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolvePluginDependencies(Artifact pluginArtifact) throws BootstrapMavenException {
        try {
            return repoSystem.resolveDependencies(repoSession, new DependencyRequest().setCollectRequest(new CollectRequest()
                    .setRoot(new Dependency(pluginArtifact, null)).setRepositories(context.getRemotePluginRepositories())));
        } catch (DependencyResolutionException e) {
            throw new BootstrapMavenException("Failed to resolve dependencies for Maven plugin " + pluginArtifact, e);
        }
    }

    /**
     * Turns the list of dependencies into a simple dependency tree
     */
    public DependencyResult toDependencyTree(List<Dependency> deps, List<RemoteRepository> mainRepos)
            throws BootstrapMavenException {
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
            List<RemoteRepository> mainRepos, Collection<Exclusion> exclusions, String... excludedScopes)
            throws BootstrapMavenException {
        try {
            return repoSystem.collectDependencies(repoSession,
                    newCollectManagedRequest(artifact, deps, managedDeps, mainRepos, exclusions, excludedScopes));
        } catch (DependencyCollectionException e) {
            throw new BootstrapMavenException("Failed to collect dependencies for " + artifact, e);
        }
    }

    private CollectRequest newCollectManagedRequest(Artifact artifact, List<Dependency> deps, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, String... excludedScopes) throws BootstrapMavenException {
        return newCollectManagedRequest(artifact, deps, managedDeps, mainRepos, Collections.emptyList(), excludedScopes);
    }

    private CollectRequest newCollectManagedRequest(Artifact artifact, List<Dependency> deps, List<Dependency> managedDeps,
            List<RemoteRepository> mainRepos, Collection<Exclusion> exclusions, String... excludedScopes)
            throws BootstrapMavenException {
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

        final CollectRequest request = new CollectRequest()
                .setDependencies(mergeDeps(deps, originalDeps, managedVersions))
                .setManagedDependencies(mergedManagedDeps)
                .setRepositories(aggregateRepositories(aggregatedRepos, newResolutionRepositories(descr.getRepositories())));
        if (exclusions.isEmpty()) {
            request.setRootArtifact(artifact);
        } else {
            request.setRoot(new Dependency(artifact, JavaScopes.COMPILE, false, exclusions));
        }
        return request;
    }

    public List<RemoteRepository> newResolutionRepositories(List<RemoteRepository> repos) {
        return repos.isEmpty() ? Collections.emptyList() : repoSystem.newResolutionRepositories(repoSession, repos);
    }

    public List<RemoteRepository> aggregateRepositories(List<RemoteRepository> dominant, List<RemoteRepository> recessive) {
        return dominant.isEmpty() ? recessive
                : remoteRepoManager.aggregateRepositories(repoSession, dominant, recessive, false);
    }

    public void install(Artifact artifact) throws BootstrapMavenException {
        try {
            repoSystem.install(repoSession, new InstallRequest().addArtifact(artifact));
        } catch (InstallationException ex) {
            throw new BootstrapMavenException("Failed to install " + artifact, ex);
        }
    }

    private CollectRequest newCollectRequest(Artifact artifact, List<RemoteRepository> mainRepos) {
        return newCollectRequest(artifact, mainRepos, Collections.emptyList());
    }

    private CollectRequest newCollectRequest(Artifact artifact, List<RemoteRepository> mainRepos,
            Collection<Exclusion> exclusions) {
        return new CollectRequest()
                .setRoot(new Dependency(artifact, JavaScopes.RUNTIME, false, exclusions))
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
