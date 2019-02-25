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

package io.quarkus.bootstrap.resolver;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.Version;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.maven.DeploymentInjectingDependencyVisitor;
import io.quarkus.bootstrap.resolver.maven.DeploymentInjectionException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.SimpleDependencyGraphTransformationContext;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapAppModelResolver implements AppModelResolver {

    protected final MavenArtifactResolver mvn;

    public BootstrapAppModelResolver(MavenArtifactResolver mvn) throws AppModelResolverException {
        this.mvn = mvn;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        mvn.addRemoteRepositories(repos);
    }

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppModelResolverException {
        if(mvn.getLocalRepositoryManager() == null) {
            return;
        }
        mvn.getLocalRepositoryManager().relink(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion(), path);
        artifact.setPath(path);
    }

    @Override
    public Path resolve(AppArtifact artifact) throws AppModelResolverException {
        if(artifact.isResolved()) {
            return artifact.getPath();
        }
        final Path path = mvn.resolve(toAetherArtifact(artifact)).getArtifact().getFile().toPath();
        artifact.setPath(path);
        return path;
    }

    @Override
    public AppModel resolveModel(AppArtifact coords) throws AppModelResolverException {
        return injectDeploymentDependencies(coords, mvn.resolveDependencies(toAetherArtifact(coords)).getRoot());
    }

    @Override
    public AppModel resolveModel(AppArtifact root, List<AppDependency> coords) throws AppModelResolverException {
        final List<Dependency> mvnDeps = new ArrayList<>(coords.size());
        for(AppDependency dep : coords) {
            mvnDeps.add(new Dependency(toAetherArtifact(dep.getArtifact()), dep.getScope()));
        }
        return injectDeploymentDependencies(root, mvn.resolveDependencies(toAetherArtifact(root), mvnDeps).getRoot());
    }

    @Override
    public List<String> listLaterVersions(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, upToVersion, inclusive);
        final List<Version> resolvedVersions = rangeResult.getVersions();
        final List<String> versions = new ArrayList<>(resolvedVersions.size());
        for (Version v : resolvedVersions) {
            versions.add(v.toString());
        }
        return versions;
    }

    @Override
    public String getNextVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppModelResolverException {
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
    public String getLatestVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppModelResolverException {
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

    public List<RemoteRepository> resolveArtifactRepos(AppArtifact appArtifact) throws AppModelResolverException {
        return mvn.resolveDescriptor(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(), appArtifact.getType(), appArtifact.getVersion())).getRepositories();
    }

    public void install(AppArtifact appArtifact, Path localPath) throws AppModelResolverException {
        mvn.install(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(),
                appArtifact.getType(), appArtifact.getVersion(), Collections.emptyMap(), localPath.toFile()));
    }

    private AppModel injectDeploymentDependencies(AppArtifact appArtifact, DependencyNode root) throws AppModelResolverException {

        final Set<AppArtifactKey> appDeps = new HashSet<>();
        final List<AppDependency> userDeps = new ArrayList<>();
        TreeDependencyVisitor visitor = new TreeDependencyVisitor(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                final Dependency dep = node.getDependency();
                if(dep != null) {
                    final AppArtifact appArtifact = BootstrapAppModelResolver.toAppArtifact(dep.getArtifact());
                    appDeps.add(appArtifact.getKey());
                    userDeps.add(new AppDependency(appArtifact, dep.getScope(), dep.isOptional()));
                }
                return true;
            }});
        for(DependencyNode child : root.getChildren()) {
            child.accept(visitor);
        }

        final DeploymentInjectingDependencyVisitor deploymentInjector = new DeploymentInjectingDependencyVisitor(mvn);
        try {
            root.accept(new TreeDependencyVisitor(deploymentInjector));
        } catch (DeploymentInjectionException e) {
            throw new AppModelResolverException("Failed to inject extension deployment dependencies for " + root.getArtifact(), e.getCause());
        }

        List<AppDependency> deploymentDeps = Collections.emptyList();
        if(deploymentInjector.isInjectedDeps()) {
            final DependencyGraphTransformationContext context = new SimpleDependencyGraphTransformationContext(mvn.getSession());
            try {
                // add conflict IDs to the added deployments
                root = new ConflictMarker().transformGraph(root, context);
                // resolves version conflicts
                root = new ConflictIdSorter().transformGraph(root, context);
                root = mvn.getSession().getDependencyGraphTransformer().transformGraph(root, context);
            } catch (RepositoryException e) {
                throw new AppModelResolverException("Failed to normalize the dependency graph", e);
            }
            List<DependencyNode> deploymentDepNodes = new ArrayList<>();
            final List<ArtifactRequest> requests = new ArrayList<>();
            visitor = new TreeDependencyVisitor(new DependencyVisitor() {
                DependencyNode deploymentNode;
                DependencyNode runtimeNode;
                Artifact runtimeArtifact;
                @Override
                public boolean visitEnter(DependencyNode node) {
                    final Dependency dep = node.getDependency();
                    if(dep != null) {
                        if(deploymentNode == null) {
                            runtimeArtifact = DeploymentInjectingDependencyVisitor.getInjectedDependency(node);
                            if(runtimeArtifact != null) {
                                deploymentNode = node;
                            }
                        } else if(runtimeArtifact != null && runtimeNode == null && runtimeArtifact.equals(dep.getArtifact())) {
                            runtimeNode = node;
                        }
                    }
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode node) {
                    final Dependency dep = node.getDependency();
                    if (dep != null) {
                        final Artifact artifact = dep.getArtifact();
                        if (artifact.getFile() == null) {
                            requests.add(new ArtifactRequest(node));
                        }
                        if (deploymentNode != null) {
                            if (runtimeNode == null && !appDeps.contains(new AppArtifactKey(artifact.getGroupId(),
                                    artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension()))) {
                                deploymentDepNodes.add(node);
                            } else if (runtimeNode == node) {
                                runtimeNode = null;
                                runtimeArtifact = null;
                            }
                            if (deploymentNode == node) {
                                deploymentNode = null;
                            }
                        }
                    }
                    return true;
                }
            });
            for(DependencyNode child : root.getChildren()) {
                child.accept(visitor);
            }
            if(!requests.isEmpty()) {
                final List<ArtifactResult> results = mvn.resolve(requests);
                // update the artifacts in the graph
                for (ArtifactResult result : results) {
                    final Artifact artifact = result.getArtifact();
                    if (artifact != null) {
                        result.getRequest().getDependencyNode().setArtifact(artifact);
                    }
                }
                deploymentDeps = new ArrayList<>(deploymentDepNodes.size());
                for (DependencyNode dep : deploymentDepNodes) {
                    deploymentDeps.add(new AppDependency(BootstrapAppModelResolver.toAppArtifact(dep.getArtifact()),
                            dep.getDependency().getScope(), dep.getDependency().isOptional()));
                }
            }
        }

        return new AppModel(appArtifact, userDeps, deploymentDeps);
    }

    private VersionRangeResult resolveVersionRangeResult(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        return mvn.resolveVersionRange(new DefaultArtifact(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getType(),
                '(' + appArtifact.getVersion() + ',' + (upToVersion == null ? ')' : upToVersion + (inclusive ? ']' : ')'))));
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

    private static AppArtifact toAppArtifact(Artifact artifact) {
        final AppArtifact appArtifact = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        final File file = artifact.getFile();
        if(file != null) {
            appArtifact.setPath(file.toPath());
        }
        return appArtifact;
    }
}
