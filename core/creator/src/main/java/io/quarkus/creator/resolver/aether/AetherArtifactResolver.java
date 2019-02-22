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

package io.quarkus.creator.resolver.aether;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
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
import org.eclipse.aether.version.Version;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppArtifactResolverBase;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class AetherArtifactResolver extends AppArtifactResolverBase {

    public static AetherArtifactResolver getInstance(Path repoHome) throws AppCreatorException {
        final RepositorySystem repoSystem = MavenRepoInitializer.getRepositorySystem();
        final Settings settings = MavenRepoInitializer.getSettings();
        final DefaultRepositorySystemSession repoSession = MavenRepoInitializer.newSession(repoSystem, settings);
        final AppCreatorLocalRepositoryManager appCreatorLocalRepoManager = new AppCreatorLocalRepositoryManager(
                repoSystem.newLocalRepositoryManager(repoSession,
                        new LocalRepository(repoHome.toString())),
                Paths.get(MavenRepoInitializer.getLocalRepo(settings)));
        repoSession.setLocalRepositoryManager(appCreatorLocalRepoManager);
        repoSession.setDependencySelector(new AppCreatorDependencySelector(true));
        final AetherArtifactResolver resolver = new AetherArtifactResolver(repoSystem, repoSession,
                MavenRepoInitializer.getRemoteRepos(settings));
        resolver.setLocalRepositoryManager(appCreatorLocalRepoManager);
        return resolver;
    }

    public static AetherArtifactResolver getInstance(Path repoHome, List<RemoteRepository> remoteRepos)
            throws AppCreatorException {
        final RepositorySystem repoSystem = MavenRepoInitializer.getRepositorySystem();
        final Settings settings = MavenRepoInitializer.getSettings();
        final DefaultRepositorySystemSession repoSession = MavenRepoInitializer.newSession(repoSystem, settings);
        final AppCreatorLocalRepositoryManager appCreatorLocalRepoManager = new AppCreatorLocalRepositoryManager(
                repoSystem.newLocalRepositoryManager(repoSession,
                        new LocalRepository(repoHome.toString())),
                Paths.get(MavenRepoInitializer.getLocalRepo(settings)));
        repoSession.setLocalRepositoryManager(appCreatorLocalRepoManager);
        repoSession.setDependencySelector(new AppCreatorDependencySelector(true));
        final AetherArtifactResolver resolver = new AetherArtifactResolver(repoSystem, repoSession, remoteRepos);
        resolver.setLocalRepositoryManager(appCreatorLocalRepoManager);
        return resolver;
    }

    protected final RepositorySystem repoSystem;
    protected final RepositorySystemSession repoSession;
    protected final List<RemoteRepository> remoteRepos;
    protected AppCreatorLocalRepositoryManager localRepoManager;

    public AetherArtifactResolver() throws AppCreatorException {
        this(MavenRepoInitializer.getRepositorySystem(),
                MavenRepoInitializer.newSession(MavenRepoInitializer.getRepositorySystem()),
                MavenRepoInitializer.getRemoteRepos());
    }

    public AetherArtifactResolver(RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepos) {
        super();
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
    }

    public void setLocalRepositoryManager(AppCreatorLocalRepositoryManager localRepoManager) {
        this.localRepoManager = localRepoManager;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        remoteRepos.addAll(repos);
    }

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppCreatorException {
        if (localRepoManager == null) {
            throw new AppCreatorException("Failed to (re-)link " + artifact + " to " + path
                    + ": AppCreatorLocalRepositoryManager has not been initialized");
        }
        localRepoManager.relink(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(),
                artifact.getVersion(), path);
        setPath(artifact, path);
    }

    @Override
    protected void doResolve(AppArtifact artifact) throws AppCreatorException {
        final ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(toAetherArtifact(artifact));
        artifactRequest.setRepositories(remoteRepos);
        ArtifactResult artifactResult;
        try {
            artifactResult = repoSystem.resolveArtifact(repoSession, artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new AppCreatorException("Failed to resolve artifact " + artifact, e);
        }
        setPath(artifact, artifactResult.getArtifact().getFile().toPath());
    }

    @Override
    public List<AppDependency> collectDependencies(AppArtifact coords) throws AppCreatorException {
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(toAetherArtifact(coords), "runtime"));
        //collectRequest.setRootArtifact(toAetherArtifact(coords));
        collectRequest.setRepositories(remoteRepos);

        final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);

        final DependencyResult depResult;
        try {
            depResult = repoSystem.resolveDependencies(repoSession, dependencyRequest);
        } catch (DependencyResolutionException e) {
            throw new AppCreatorException("Failed to collect dependencies for " + coords, e);
        }

        final List<DependencyNode> depNodes = depResult.getRoot().getChildren();
        if (depNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<AppDependency> appDeps = new ArrayList<>();
        collect(depNodes, appDeps);
        return appDeps;
    }

    @Override
    public List<AppDependency> collectDependencies(AppArtifact root, List<AppDependency> coords) throws AppCreatorException {
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(toAetherArtifact(root), "runtime"));
        for (AppDependency dep : coords) {
            collectRequest.addDependency(new Dependency(toAetherArtifact(dep.getArtifact()), dep.getScope()));
        }
        collectRequest.setRepositories(remoteRepos);

        final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);

        final DependencyResult depResult;
        try {
            depResult = repoSystem.resolveDependencies(repoSession, dependencyRequest);
        } catch (DependencyResolutionException e) {
            throw new AppCreatorException("Failed to collect dependencies for " + coords, e);
        }

        final List<DependencyNode> depNodes = depResult.getRoot().getChildren();
        if (depNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<AppDependency> appDeps = new ArrayList<>();
        collect(depNodes, appDeps);
        return appDeps;
    }

    /*
     * @Override
     * public List<AppDependency> collectDependencies(AppArtifact coords) throws AppCreatorException {
     * final CollectRequest collectRequest = new CollectRequest();
     * collectRequest.setRoot(new Dependency(toAetherArtifact(coords), "runtime"));
     * //collectRequest.setRootArtifact(toAetherArtifact(coords));
     * collectRequest.setRepositories(remoteRepos);
     * 
     * final CollectResult depResult;
     * try {
     * depResult = repoSystem.collectDependencies(repoSession, collectRequest);
     * } catch (DependencyCollectionException e) {
     * throw new AppCreatorException("Failed to collect dependencies for " + coords, e);
     * }
     * 
     * final List<DependencyNode> depNodes = depResult.getRoot().getChildren();
     * if(depNodes.isEmpty()) {
     * return Collections.emptyList();
     * }
     * 
     * final List<AppDependency> appDeps = new ArrayList<>();
     * collect(depNodes, appDeps);
     * return appDeps;
     * }
     */
    @Override
    public List<String> listLaterVersions(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppCreatorException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> resolvedVersions = rangeResult.getVersions();
        final List<String> versions = new ArrayList<>(resolvedVersions.size());
        for (Version v : resolvedVersions) {
            versions.add(v.toString());
        }
        return versions;
    }

    @Override
    public String getNextVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppCreatorException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> versions = rangeResult.getVersions();
        if (versions.isEmpty()) {
            return appArtifact.getVersion();
        }
        Version next = versions.get(0);
        for (int i = 1; i < versions.size(); ++i) {
            final Version candidate = versions.get(i);
            if (next.compareTo(candidate) > 0) {
                next = candidate;
            }
        }
        return next.toString();
    }

    @Override
    public String getLatestVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppCreatorException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> versions = rangeResult.getVersions();
        if (versions.isEmpty()) {
            return appArtifact.getVersion();
        }
        Version latest = versions.get(0);
        for (int i = 1; i < versions.size(); ++i) {
            final Version candidate = versions.get(i);
            if (latest.compareTo(candidate) < 0) {
                latest = candidate;
            }
        }
        return latest.toString();
    }

    public List<RemoteRepository> resolveArtifactRepos(AppArtifact appArtifact) throws AppCreatorException {
        final ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                appArtifact.getClassifier(), appArtifact.getType(), appArtifact.getVersion()));
        //request.setRepositories(remoteRepos);
        final ArtifactDescriptorResult result;
        try {
            result = repoSystem.readArtifactDescriptor(repoSession, request);
        } catch (ArtifactDescriptorException e) {
            throw new AppCreatorException("Failed to resolve descriptor for " + appArtifact, e);
        }
        return result.getRepositories();
    }

    private VersionRangeResult resolveVersionRangeResult(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppCreatorException {
        final Artifact artifact = new DefaultArtifact(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getType(),
                '(' + appArtifact.getVersion() + ',' + (upToVersion == null ? ')' : upToVersion + (inclusive ? ']' : ')')));
        //System.out.println("AetherArtifactResolver.listLaterVersions for range " + artifact.getVersion());
        final VersionRangeResult rangeResult = getVersionRange(artifact);
        return rangeResult;
    }

    public void install(AppArtifact appArtifact, Path localPath) throws AppCreatorException {
        final InstallRequest request = new InstallRequest();
        request.addArtifact(
                new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(),
                        appArtifact.getType(), appArtifact.getVersion(), Collections.emptyMap(), localPath.toFile()));
        try {
            repoSystem.install(repoSession, request);
        } catch (InstallationException ex) {
            throw new AppCreatorException("Failed to install " + appArtifact, ex);
        }
    }

    private VersionRangeResult getVersionRange(Artifact artifact) throws AppCreatorException {
        final VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(remoteRepos);
        VersionRangeResult rangeResult;
        try {
            rangeResult = repoSystem.resolveVersionRange(repoSession, rangeRequest);
        } catch (VersionRangeResolutionException ex) {
            throw new AppCreatorException("Failed to resolve version range for " + artifact, ex);
        }
        return rangeResult;
    }

    private static void collect(List<DependencyNode> nodes, List<AppDependency> appDeps) {
        for (DependencyNode node : nodes) {
            collect(node.getChildren(), appDeps);
            appDeps.add(new AppDependency(toAppArtifact(node.getArtifact()), node.getDependency().getScope()));
        }
    }

    private static Artifact toAetherArtifact(AppArtifact artifact) {
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getType(), artifact.getVersion());
    }

    private static AppArtifact toAppArtifact(Artifact artifact) {
        final AppArtifact mvn = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getExtension(), artifact.getVersion());
        final File file = artifact.getFile();
        if (file != null) {
            setPath(mvn, file.toPath());
        }
        return mvn;
    }
}
