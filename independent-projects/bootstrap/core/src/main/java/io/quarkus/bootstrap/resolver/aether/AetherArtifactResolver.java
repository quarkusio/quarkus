/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap.resolver.aether;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
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
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.Version;
import io.quarkus.bootstrap.resolver.AppArtifact;
import io.quarkus.bootstrap.resolver.AppArtifactResolverException;
import io.quarkus.bootstrap.resolver.AppDependencies;
import io.quarkus.bootstrap.resolver.AppArtifactResolverBase;
import io.quarkus.bootstrap.resolver.AppDependency;
import io.quarkus.bootstrap.resolver.workspace.LocalWorkspace;

/**
 *
 * @author Alexey Loubyansky
 */
public class AetherArtifactResolver extends AppArtifactResolverBase {

    public static class Builder {

        private Path repoHome;
        private RepositorySystem repoSystem;
        private RepositorySystemSession repoSession;
        private List<RemoteRepository> remoteRepos = Collections.emptyList();
        private boolean offline;
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

        public AetherArtifactResolver build() throws AppArtifactResolverException {
            return new AetherArtifactResolver(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected final RepositorySystem repoSystem;
    protected final RepositorySystemSession repoSession;
    protected final List<RemoteRepository> remoteRepos;
    protected final BootstrapLocalRepositoryManager localRepoManager;
    private final ConflictMarker conflictMarker = new ConflictMarker();
    private final ConflictIdSorter conflictIdSorter = new ConflictIdSorter();

    private AetherArtifactResolver(Builder builder) throws AppArtifactResolverException {
        this.repoSystem = builder.repoSystem == null ? MavenRepoInitializer.getRepositorySystem(builder.workspace) : builder.repoSystem;
        final DefaultRepositorySystemSession newSession = builder.repoSession == null ? MavenRepoInitializer.newSession(repoSystem) : new DefaultRepositorySystemSession(builder.repoSession);
        newSession.setOffline(builder.offline);

        if(builder.repoHome != null) {
            final BootstrapLocalRepositoryManager appCreatorLocalRepoManager = new BootstrapLocalRepositoryManager(
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
        this.remoteRepos = builder.remoteRepos;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        remoteRepos.addAll(repos);
    }

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppArtifactResolverException {
        if(localRepoManager == null) {
            return;
        }
        localRepoManager.relink(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion(), path);
        setPath(artifact, path);
    }

    @Override
    protected void doResolve(AppArtifact artifact) throws AppArtifactResolverException {
        setPath(artifact, doResolve(toAetherArtifact(artifact)).getArtifact().getFile().toPath());
    }

    protected ArtifactResult doResolve(Artifact aetherArtifact) throws AppArtifactResolverException {
        final ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(aetherArtifact);
        artifactRequest.setRepositories(remoteRepos);
        try {
            return repoSystem.resolveArtifact(repoSession, artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new AppArtifactResolverException("Failed to resolve artifact " + aetherArtifact, e);
        }
    }

    @Override
    public AppDependencies collectDependencies(AppArtifact coords) throws AppArtifactResolverException {
        final Artifact artifact = toAetherArtifact(coords);
        final ArtifactDescriptorResult artDescr = resolveDescriptor(artifact);

        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(artifact);
        collectRequest.setDependencies(artDescr.getDependencies());
        collectRequest.setManagedDependencies(artDescr.getManagedDependencies());
        collectRequest.setRepositories(artDescr.getRepositories());

        final DependencyNode root;
        try {
            root = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            throw new AppArtifactResolverException("Failed to collect dependencies for " + coords, e);
        }
        return injectDeploymentDependencies(root);
    }

    @Override
    public AppDependencies collectRuntimeDependencies(AppArtifact coords) throws AppArtifactResolverException {
        final Artifact artifact = toAetherArtifact(coords);
        final ArtifactDescriptorResult artDescr = resolveDescriptor(artifact);

        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "runtime"));
        collectRequest.setRepositories(artDescr.getRepositories());

        final DependencyNode root;
        try {
            root = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            throw new AppArtifactResolverException("Failed to collect dependencies for " + coords, e);
        }
        return injectDeploymentDependencies(root);
    }

    @Override
    public AppDependencies collectDependencies(AppArtifact root, List<AppDependency> coords) throws AppArtifactResolverException {
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(toAetherArtifact(root), "runtime"));
        for(AppDependency dep : coords) {
            collectRequest.addDependency(new Dependency(toAetherArtifact(dep.getArtifact()), dep.getScope()));
        }
        collectRequest.setRepositories(remoteRepos);
        final DependencyNode node;
        try {
            node = repoSystem.resolveDependencies(repoSession, new DependencyRequest(collectRequest, null)).getRoot();
        } catch (DependencyResolutionException e) {
            throw new AppArtifactResolverException("Failed to collect dependencies for " + coords, e);
        }
        return injectDeploymentDependencies(node);
    }

    @Override
    public List<String> listLaterVersions(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppArtifactResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> resolvedVersions = rangeResult.getVersions();
        final List<String> versions = new ArrayList<>(resolvedVersions.size());
        for (Version v : resolvedVersions) {
            versions.add(v.toString());
        }
        return versions;
    }

    @Override
    public String getNextVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppArtifactResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> versions = rangeResult.getVersions();
        if(versions.isEmpty()) {
            return appArtifact.getVersion();
        }
        Version next = versions.get(0);
        for(int i = 1; i < versions.size(); ++i) {
            final Version candidate = versions.get(i);
            if(next.compareTo(candidate) > 0) {
                next = candidate;
            }
        }
        return next.toString();
    }

    @Override
    public String getLatestVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppArtifactResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> versions = rangeResult.getVersions();
        if(versions.isEmpty()) {
            return appArtifact.getVersion();
        }
        Version latest = versions.get(0);
        for(int i = 1; i < versions.size(); ++i) {
            final Version candidate = versions.get(i);
            if(latest.compareTo(candidate) < 0) {
                latest = candidate;
            }
        }
        return latest.toString();
    }

    public List<RemoteRepository> resolveArtifactRepos(AppArtifact appArtifact) throws AppArtifactResolverException {
        return resolveDescriptor(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(), appArtifact.getType(), appArtifact.getVersion())).getRepositories();
    }

    public void install(AppArtifact appArtifact, Path localPath) throws AppArtifactResolverException {
        final InstallRequest request = new InstallRequest();
        request.addArtifact(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(),
                appArtifact.getType(), appArtifact.getVersion(), Collections.emptyMap(), localPath.toFile()));
        try {
            repoSystem.install(repoSession, request);
        } catch (InstallationException ex) {
            throw new AppArtifactResolverException("Failed to install " + appArtifact, ex);
        }
    }

    private AppDependencies injectDeploymentDependencies(DependencyNode root) throws AppArtifactResolverException {
        final DeploymentInjectingDependencyVisitor deploymentInjector = new DeploymentInjectingDependencyVisitor(this);
        try {
            root.accept(new TreeDependencyVisitor(deploymentInjector));
        } catch (DeploymentInjectionException e) {
            throw new AppArtifactResolverException("Failed to inject extension deployment dependencies for " + root.getArtifact(), e.getCause());
        }

        if(deploymentInjector.injectedDeps) {
            final DependencyGraphTransformationContext context = new SimpleDependencyGraphTransformationContext(repoSession);
            try {
                // add conflict IDs to the added deployments
                root = conflictMarker.transformGraph(root, context);
                // resolves version conflicts
                root = conflictIdSorter.transformGraph(root, context);
                root = repoSession.getDependencyGraphTransformer().transformGraph(root, context);
            } catch (RepositoryException e) {
                throw new AppArtifactResolverException("Failed to normalize the dependency graph", e);
            }
            final List<ArtifactRequest> requests = new ArrayList<>();
            root.accept(new TreeDependencyVisitor(new DependencyVisitor() {
                @Override
                public boolean visitEnter(DependencyNode node) {
                    final Dependency dep = node.getDependency();
                    if(dep != null && dep.getArtifact().getFile() == null) {
                        requests.add(new ArtifactRequest(node));
                    }
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode node) {
                    return true;
                }
            }));
            if(!requests.isEmpty()) {
                final List<ArtifactResult> results;
                try {
                    results = repoSystem.resolveArtifacts(repoSession, requests);
                } catch (ArtifactResolutionException e) {
                    throw new AppArtifactResolverException("Failed to resolve collected dependencies", e);
                }
                // update the artifacts in the graph
                for (ArtifactResult result : results) {
                    final Artifact artifact = result.getArtifact();
                    if (artifact != null) {
                        result.getRequest().getDependencyNode().setArtifact(artifact);
                    }
                }
            }
        }

        return new AetherAppCollectedDependencies(this, root, deploymentInjector.injectedDeps);
    }

    DependencyNode collectDependencies(Artifact artifact) throws AppArtifactResolverException {
        final ArtifactDescriptorResult artDescr = resolveDescriptor(artifact);
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, "runtime"));
        collectRequest.setRepositories(artDescr.getRepositories());
        try {
            return repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            throw new AppArtifactResolverException("Failed to collect dependencies for " + artifact, e);
        }
    }

    private VersionRangeResult resolveVersionRangeResult(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppArtifactResolverException {
        final Artifact artifact = new DefaultArtifact(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getType(),
                '(' + appArtifact.getVersion() + ',' + (upToVersion == null ? ')' : upToVersion + (inclusive ? ']' : ')')));
        //System.out.println("AetherArtifactResolver.listLaterVersions for range " + artifact.getVersion());
        return getVersionRange(artifact);
    }

    private VersionRangeResult getVersionRange(Artifact artifact) throws AppArtifactResolverException {
        final VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(remoteRepos);
        try {
            return repoSystem.resolveVersionRange(repoSession, rangeRequest);
        } catch (VersionRangeResolutionException ex) {
            throw new AppArtifactResolverException("Failed to resolve version range for " + artifact, ex);
        }
    }

    private ArtifactDescriptorResult resolveDescriptor(final Artifact artifact)
            throws AppArtifactResolverException {
        final ArtifactDescriptorRequest descrReq = new ArtifactDescriptorRequest();
        descrReq.setArtifact(artifact);
        try {
            return repoSystem.readArtifactDescriptor(repoSession, descrReq);
        } catch (ArtifactDescriptorException e) {
            throw new AppArtifactResolverException("Failed to read descriptor of " + artifact, e);
        }
    }

    static List<AppDependency> toAppDepList(DependencyNode rootNode) {
        final List<DependencyNode> depNodes = rootNode.getChildren();
        if(depNodes.isEmpty()) {
            return Collections.emptyList();
        }
        final List<AppDependency> appDeps =  new ArrayList<>();
        collect(depNodes, appDeps);
        return appDeps;
    }

    private static void collect(List<DependencyNode> nodes, List<AppDependency> appDeps) {
        for(DependencyNode node : nodes) {
            collect(node.getChildren(), appDeps);
            final Dependency dep = node.getDependency();
            appDeps.add(new AppDependency(toAppArtifact(node.getArtifact()), dep.getScope(), dep.isOptional()));
        }
    }

    private static Artifact toAetherArtifact(AppArtifact artifact) {
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion());
    }

    static AppArtifact toAppArtifact(Artifact artifact) {
        final AppArtifact mvn = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        final File file = artifact.getFile();
        if(file != null) {
            setPath(mvn, file.toPath());
        }
        return mvn;
    }
}
