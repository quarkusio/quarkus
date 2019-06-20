package io.quarkus.bootstrap.resolver;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
import io.quarkus.bootstrap.resolver.maven.BuildDependencyGraphVisitor;
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
    protected Consumer<String> buildTreeConsumer;
    protected boolean devmode;

    public BootstrapAppModelResolver(MavenArtifactResolver mvn) throws AppModelResolverException {
        this.mvn = mvn;
    }

    public void setBuildTreeLogger(Consumer<String> buildTreeConsumer) {
        this.buildTreeConsumer = buildTreeConsumer;
    }

    /**
     * Indicates whether application should be resolved to set up the dev mode.
     * The important difference between the dev mode and the usual build is that
     * in the dev mode the user application will have to be compiled, so the classpath
     * will have to include dependencies of scope provided.
     *
     * @param devmode  whether the resolver is going to be used to set up the dev mode
     */
    public BootstrapAppModelResolver setDevMode(boolean devmode) {
        this.devmode = devmode;
        return this;
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
    public List<AppDependency> readManagedDependencies(AppArtifact artifact) throws AppModelResolverException {
        final List<Dependency> aetherDeps = mvn.resolveDescriptor(toAetherArtifact(artifact)).getManagedDependencies();
        if(aetherDeps.isEmpty()) {
            return Collections.emptyList();
        }
        final List<AppDependency> result = new ArrayList<>(aetherDeps.size());
        for(Dependency aetherDep : aetherDeps) {
            result.add(new AppDependency(new AppArtifact(
                    aetherDep.getArtifact().getGroupId(),
                    aetherDep.getArtifact().getArtifactId(),
                    aetherDep.getArtifact().getClassifier(),
                    aetherDep.getArtifact().getExtension(),
                    aetherDep.getArtifact().getVersion()
                    ),
                    aetherDep.getScope(), aetherDep.isOptional()));
        }
        return result;
    }

    @Override
    public List<AppDependency> resolveUserDependencies(AppArtifact appArtifact, List<AppDependency> deps) throws AppModelResolverException {
        final List<Dependency> mvnDeps;
        if(deps.isEmpty()) {
            mvnDeps = Collections.emptyList();
        } else {
            mvnDeps = new ArrayList<>(deps.size());
            for (AppDependency dep : deps) {
                mvnDeps.add(new Dependency(toAetherArtifact(dep.getArtifact()), dep.getScope()));
            }
        }
        final List<AppDependency> result = new ArrayList<>();
        final TreeDependencyVisitor visitor = new TreeDependencyVisitor(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                final Dependency dep = node.getDependency();
                if(dep != null) {
                    result.add(new AppDependency(toAppArtifact(dep.getArtifact()), dep.getScope(), dep.isOptional()));
                }
                return true;
            }});
        mvn.resolveDependencies(toAetherArtifact(appArtifact), mvnDeps).getRoot().accept(visitor);
        return result;
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact) throws AppModelResolverException {
        return resolveManagedModel(appArtifact, Collections.emptyList(), readManagedDependencies(appArtifact));
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact, List<AppDependency> directDeps) throws AppModelResolverException {
        return resolveManagedModel(appArtifact, directDeps, readManagedDependencies(appArtifact));
    }

    public AppModel resolveManagedModel(AppArtifact appArtifact, List<AppDependency> directDeps, List<AppDependency> managedDeps) throws AppModelResolverException {
        return doResolveModel(appArtifact, toAetherDeps(directDeps), toAetherDeps(managedDeps));
    }

    private AppModel doResolveModel(AppArtifact appArtifact, List<Dependency> directMvnDeps, List<Dependency> managedMvnDeps) throws AppModelResolverException {
        return injectDeploymentDependencies(appArtifact, mvn.resolveManagedDependencies(toAetherArtifact(appArtifact),
                directMvnDeps, managedMvnDeps, devmode ? new String[] { "test" } : new String[0]).getRoot(), managedMvnDeps);
    }

    @Override
    public List<String> listLaterVersions(AppArtifact appArtifact, String upToVersion, boolean inclusive) throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, appArtifact.getVersion(), false, upToVersion, inclusive);
        final List<Version> resolvedVersions = rangeResult.getVersions();
        final List<String> versions = new ArrayList<>(resolvedVersions.size());
        for (Version v : resolvedVersions) {
            versions.add(v.toString());
        }
        return versions;
    }

    @Override
    public String getNextVersion(AppArtifact appArtifact, String fromVersion, boolean fromVersionIncluded, String upToVersion, boolean upToVersionInclusive) throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, fromVersion, fromVersionIncluded, upToVersion, upToVersionInclusive);
        final List<Version> versions = rangeResult.getVersions();
        if(versions.isEmpty()) {
            return null;
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
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, appArtifact.getVersion(), false, upToVersion, inclusive);
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
        return mvn.resolveDescriptor(toAetherArtifact(appArtifact)).getRepositories();
    }

    public void install(AppArtifact appArtifact, Path localPath) throws AppModelResolverException {
        mvn.install(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(),
                appArtifact.getType(), appArtifact.getVersion(), Collections.emptyMap(), localPath.toFile()));
    }

    private AppModel injectDeploymentDependencies(AppArtifact appArtifact, DependencyNode root, List<Dependency> managedDeps) throws AppModelResolverException {

        final Set<AppArtifactKey> appDeps = new HashSet<>();
        final List<AppDependency> userDeps = new ArrayList<>();
        final TreeDependencyVisitor visitor = new TreeDependencyVisitor(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                final Dependency dep = node.getDependency();
                if(dep != null) {
                    final AppArtifact appArtifact = toAppArtifact(dep.getArtifact());
                    appDeps.add(appArtifact.getKey());
                    userDeps.add(new AppDependency(appArtifact, dep.getScope(), dep.isOptional()));
                }
                return true;
            }});
        for(DependencyNode child : root.getChildren()) {
            child.accept(visitor);
        }

        final DeploymentInjectingDependencyVisitor deploymentInjector = new DeploymentInjectingDependencyVisitor(mvn, managedDeps);
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
            final BuildDependencyGraphVisitor buildDepsVisitor = new BuildDependencyGraphVisitor(appDeps, buildTreeConsumer);
            buildDepsVisitor.visit(root);
            final List<ArtifactRequest> requests = buildDepsVisitor.getArtifactRequests();
            if(!requests.isEmpty()) {
                final List<ArtifactResult> results = mvn.resolve(requests);
                // update the artifacts in the graph
                for (ArtifactResult result : results) {
                    final Artifact artifact = result.getArtifact();
                    if (artifact != null) {
                        result.getRequest().getDependencyNode().setArtifact(artifact);
                    }
                }
                final List<DependencyNode> deploymentDepNodes = buildDepsVisitor.getDeploymentNodes();
                deploymentDeps = new ArrayList<>(deploymentDepNodes.size());
                for (DependencyNode dep : deploymentDepNodes) {
                    deploymentDeps.add(new AppDependency(BootstrapAppModelResolver.toAppArtifact(dep.getArtifact()),
                            dep.getDependency().getScope(), dep.getDependency().isOptional()));
                }
            }
        }

        return new AppModel(appArtifact, userDeps, deploymentDeps);
    }

    private VersionRangeResult resolveVersionRangeResult(AppArtifact appArtifact, String fromVersion, boolean fromVersionIncluded, String upToVersion, boolean upToVersionIncluded)
            throws AppModelResolverException {
        return mvn.resolveVersionRange(new DefaultArtifact(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getType(),
                (fromVersionIncluded ? '[' : '(')
                + fromVersion + ','
                + (upToVersion == null ? ')' : upToVersion + (upToVersionIncluded ? ']' : ')'))));
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

    private static List<Dependency> toAetherDeps(List<AppDependency> directDeps) {
        if(directDeps.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Dependency> directMvnDeps = new ArrayList<>(directDeps.size());
        for (AppDependency dep : directDeps) {
            directMvnDeps.add(new Dependency(toAetherArtifact(dep.getArtifact()), dep.getScope()));
        }
        return directMvnDeps;
    }
}
