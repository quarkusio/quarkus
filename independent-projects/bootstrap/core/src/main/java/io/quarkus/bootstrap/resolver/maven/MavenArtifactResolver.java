/**
 *
 */
package io.quarkus.bootstrap.resolver.maven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
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

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenArtifactResolver {

    public static class Builder {

        private Path repoHome;
        private RepositorySystem repoSystem;
        private RepositorySystemSession repoSession;
        private List<RemoteRepository> remoteRepos = null;
        private Boolean offline;
        private LocalWorkspace workspace;

        private Builder() {
        }

        public Builder setRepoHome(Path home) {
            this.repoHome = home;
            return this;
        }

        public Builder setRepositorySystem(RepositorySystem system) {
            this.repoSystem = system;
            return this;
        }

        public Builder setRepositorySystemSession(RepositorySystemSession session) {
            this.repoSession = session;
            return this;
        }

        public Builder setRemoteRepositories(List<RemoteRepository> repos) {
            this.remoteRepos = repos;
            return this;
        }

        public Builder setOffline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder setWorkspace(LocalWorkspace workspace) {
            this.workspace = workspace;
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

    private MavenArtifactResolver(Builder builder) throws AppModelResolverException {
        this.repoSystem = builder.repoSystem == null ? MavenRepoInitializer.getRepositorySystem(builder.offline, builder.workspace) : builder.repoSystem;
        final DefaultRepositorySystemSession newSession = builder.repoSession == null ? MavenRepoInitializer.newSession(repoSystem) : new DefaultRepositorySystemSession(builder.repoSession);
        if(builder.offline != null) {
            newSession.setOffline(builder.offline);
        }

        if(builder.repoHome != null) {
            final MavenLocalRepositoryManager appCreatorLocalRepoManager = new MavenLocalRepositoryManager(
                    repoSystem.newLocalRepositoryManager(newSession, new LocalRepository(builder.repoHome.toString())),
                    Paths.get(MavenRepoInitializer.getLocalRepo(MavenRepoInitializer.getSettings())));
            newSession.setLocalRepositoryManager(appCreatorLocalRepoManager);
            localRepoManager = appCreatorLocalRepoManager;
        } else {
            localRepoManager = null;
        }

        if(newSession.getCache() == null) {
            newSession.setCache(new DefaultRepositoryCache());
        }

        if (builder.workspace != null) {
            newSession.setWorkspaceReader(builder.workspace);
        }

        this.repoSession = newSession;
        this.remoteRepos = builder.remoteRepos == null ? MavenRepoInitializer.getRemoteRepos() : builder.remoteRepos;
    }

    public MavenLocalRepositoryManager getLocalRepositoryManager() {
        return localRepoManager;
    }

    public RepositorySystemSession getSession() {
        return repoSession;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        remoteRepos.addAll(repos);
    }

    public ArtifactResult resolve(Artifact artifact) throws AppModelResolverException {
        try {
            return repoSystem.resolveArtifact(repoSession,
                    new ArtifactRequest()
                    .setArtifact(artifact)
                    .setRepositories(remoteRepos));
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
        try {
            return repoSystem.readArtifactDescriptor(repoSession,
                    new ArtifactDescriptorRequest()
                    .setArtifact(artifact));
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

    public CollectResult collectDependencies(Artifact artifact) throws AppModelResolverException {
        try {
            return repoSystem.collectDependencies(repoSession, newCollectRequest(artifact));
        } catch (DependencyCollectionException e) {
            throw new AppModelResolverException("Failed to collect dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolveDependencies(Artifact artifact) throws AppModelResolverException {
        try {
            return repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest()
                    .setCollectRequest(newCollectRequest(artifact)));
        } catch (DependencyResolutionException e) {
            throw new AppModelResolverException("Failed to resolve dependencies for " + artifact, e);
        }
    }

    public CollectResult collectDependencies(Artifact artifact, List<Dependency> deps) throws AppModelResolverException {
        final CollectRequest request = newCollectRequest(artifact);
        for(Dependency dep : deps) {
            request.addDependency(dep);
        }
        try {
            return repoSystem.collectDependencies(repoSession, request);
        } catch (DependencyCollectionException e) {
            throw new AppModelResolverException("Failed to collect dependencies for " + artifact, e);
        }
    }

    public DependencyResult resolveDependencies(Artifact artifact, List<Dependency> deps) throws AppModelResolverException {
        final CollectRequest request = newCollectRequest(artifact);
        for(Dependency dep : deps) {
            request.addDependency(dep);
        }
        try {
            return repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest().setCollectRequest(request));
        } catch (DependencyResolutionException e) {
            throw new AppModelResolverException("Failed to resolve dependencies for " + artifact, e);
        }
    }

    public void install(Artifact artifact) throws AppModelResolverException {
        try {
            repoSystem.install(repoSession, new InstallRequest().addArtifact(artifact));
        } catch (InstallationException ex) {
            throw new AppModelResolverException("Failed to install " + artifact, ex);
        }
    }

    private CollectRequest newCollectRequest(Artifact artifact) throws AppModelResolverException {
        return new CollectRequest()
                .setRoot(new Dependency(artifact, JavaScopes.RUNTIME))
                .setRepositories(remoteRepos);
    }
}
