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

package org.jboss.shamrock.creator.resolver.aether;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.jboss.shamrock.creator.AppArtifact;
import org.jboss.shamrock.creator.AppArtifactResolverBase;
import org.jboss.shamrock.creator.AppCreatorException;
import org.jboss.shamrock.creator.AppDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class AetherArtifactResolver extends AppArtifactResolverBase<AetherArtifactResolver> {

    protected final RepositorySystem repoSystem;
    protected final RepositorySystemSession repoSession;
    protected final List<RemoteRepository> remoteRepos;
    protected AppCreatorLocalRepositoryManager localRepoManager;

    public AetherArtifactResolver() throws AppCreatorException {
        this(MavenRepoInitializer.getRepositorySystem(), MavenRepoInitializer.newSession(MavenRepoInitializer.getRepositorySystem()), MavenRepoInitializer.getRemoteRepos());
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

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppCreatorException {
        if(localRepoManager == null) {
            throw new AppCreatorException("Failed to (re-)link " + artifact + " to " + path + ": AppCreatorLocalRepositoryManager has not been initialized");
        }
        localRepoManager.relink(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion(), path);
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
        collectRequest.setRepositories(remoteRepos);

        final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);

        final DependencyResult depResult;
        try {
            depResult = repoSystem.resolveDependencies(repoSession, dependencyRequest);
        } catch (DependencyResolutionException e) {
            throw new AppCreatorException("Failed to collect dependencies for " + coords, e);
        }

        final List<DependencyNode> depNodes = depResult.getRoot().getChildren();
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
            appDeps.add(new AppDependency(toAppArtifact(node.getArtifact()), node.getDependency().getScope()));
        }
    }

    private static Artifact toAetherArtifact(AppArtifact artifact) {
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion());
    }

    private static AppArtifact toAppArtifact(Artifact artifact) {
        final AppArtifact mvn = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        final File file = artifact.getFile();
        if(file != null) {
            setPath(mvn, file.toPath());
        }
        return mvn;
    }
}
