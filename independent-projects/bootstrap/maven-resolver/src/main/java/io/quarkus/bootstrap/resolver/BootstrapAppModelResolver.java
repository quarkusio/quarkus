package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.maven.BuildDependencyGraphVisitor;
import io.quarkus.bootstrap.resolver.maven.DeploymentInjectingDependencyVisitor;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.SimpleDependencyGraphTransformationContext;
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
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.Version;

/**
 * @author Alexey Loubyansky
 */
public class BootstrapAppModelResolver implements AppModelResolver {

    protected final MavenArtifactResolver mvn;
    protected Consumer<String> buildTreeConsumer;
    protected boolean devmode;
    protected boolean test;

    public BootstrapAppModelResolver(MavenArtifactResolver mvn) {
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
     * @param devmode whether the resolver is going to be used to set up the dev mode
     */
    public BootstrapAppModelResolver setDevMode(boolean devmode) {
        this.devmode = devmode;
        return this;
    }

    public BootstrapAppModelResolver setTest(boolean test) {
        this.test = test;
        return this;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        mvn.addRemoteRepositories(repos);
    }

    @Override
    public void relink(AppArtifact artifact, Path path) throws AppModelResolverException {
        if (mvn.getLocalRepositoryManager() == null) {
            return;
        }
        mvn.getLocalRepositoryManager().relink(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getType(), artifact.getVersion(), path);
        artifact.setPaths(PathsCollection.of(path));
    }

    @Override
    public Path resolve(AppArtifact artifact) throws AppModelResolverException {
        if (artifact.isResolved()) {
            return artifact.getPaths().iterator().next();
        }
        final Path path = mvn.resolve(toAetherArtifact(artifact)).getArtifact().getFile().toPath();
        artifact.setPaths(PathsCollection.of(path));
        return path;
    }

    @Override
    public List<AppDependency> resolveUserDependencies(AppArtifact appArtifact, List<AppDependency> deps)
            throws AppModelResolverException {
        final List<Dependency> mvnDeps;
        if (deps.isEmpty()) {
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
                if (dep != null) {
                    result.add(new AppDependency(toAppArtifact(dep.getArtifact()), dep.getScope(), dep.isOptional()));
                }
                return true;
            }
        });
        mvn.resolveDependencies(toAetherArtifact(appArtifact), mvnDeps).getRoot().accept(visitor);
        return result;
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact) throws AppModelResolverException {
        return resolveManagedModel(appArtifact, Collections.emptyList(), null, Collections.emptySet());
    }

    @Override
    public AppModel resolveModel(AppArtifact appArtifact, List<AppDependency> directDeps) throws AppModelResolverException {
        return resolveManagedModel(appArtifact, directDeps,
                null, Collections.emptySet());
    }

    @Override
    public AppModel resolveManagedModel(AppArtifact appArtifact, List<AppDependency> directDeps, AppArtifact managingProject,
            Set<AppArtifactKey> localProjects)
            throws AppModelResolverException {
        return doResolveModel(appArtifact, toAetherDeps(directDeps), managingProject, localProjects);
    }

    private AppModel doResolveModel(AppArtifact appArtifact, List<Dependency> directMvnDeps, AppArtifact managingProject,
            Set<AppArtifactKey> localProjects)
            throws AppModelResolverException {
        if (appArtifact == null) {
            throw new IllegalArgumentException("Application artifact is null");
        }
        final Artifact mvnArtifact = toAetherArtifact(appArtifact);

        AppModel.Builder appBuilder = new AppModel.Builder().setAppArtifact(appArtifact);
        List<Dependency> managedDeps = Collections.emptyList();
        List<RemoteRepository> managedRepos = Collections.emptyList();
        if (managingProject != null) {
            final ArtifactDescriptorResult managingDescr = mvn.resolveDescriptor(toAetherArtifact(managingProject));
            managedDeps = managingDescr.getManagedDependencies();
            managedRepos = mvn.newResolutionRepositories(managingDescr.getRepositories());
        }
        List<String> excludedScopes = new ArrayList<>();
        if (!test) {
            excludedScopes.add("test");
        }
        if (!devmode) {
            excludedScopes.add("provided");
        }

        if (!appArtifact.isResolved()) {
            final ArtifactResult resolveResult = mvn.resolve(mvnArtifact, managedRepos);
            appArtifact.setPaths(PathsCollection.of(resolveResult.getArtifact().getFile().toPath()));
        }

        DependencyNode resolvedDeps = mvn.resolveManagedDependencies(mvnArtifact,
                directMvnDeps, managedDeps, managedRepos, excludedScopes.toArray(new String[0])).getRoot();

        ArtifactDescriptorResult appArtifactDescr = mvn.resolveDescriptor(toAetherArtifact(appArtifact));
        if (managingProject == null) {
            managedDeps = appArtifactDescr.getManagedDependencies();
        } else {
            final List<Dependency> mergedManagedDeps = new ArrayList<>(managedDeps.size());
            final Set<AppArtifactKey> mergedKeys = new HashSet<>(managedDeps.size());
            for (Dependency dep : managedDeps) {
                mergedKeys.add(getKey(dep.getArtifact()));
                mergedManagedDeps.add(dep);
            }
            for (Dependency dep : appArtifactDescr.getManagedDependencies()) {
                final Artifact artifact = dep.getArtifact();
                if (!mergedKeys.contains(getKey(artifact))) {
                    mergedManagedDeps.add(dep);
                }
            }
            managedDeps = mergedManagedDeps;
        }

        final List<RemoteRepository> repos = mvn.aggregateRepositories(managedRepos,
                mvn.newResolutionRepositories(appArtifactDescr.getRepositories()));

        final DeploymentInjectingDependencyVisitor deploymentInjector;
        try {
            deploymentInjector = new DeploymentInjectingDependencyVisitor(mvn, managedDeps, repos, appBuilder);
            deploymentInjector.injectDeploymentDependencies(resolvedDeps);
        } catch (BootstrapDependencyProcessingException e) {
            throw new AppModelResolverException(
                    "Failed to inject extension deployment dependencies for " + resolvedDeps.getArtifact(), e);
        }

        if (deploymentInjector.isInjectedDeps()) {
            final DependencyGraphTransformationContext context = new SimpleDependencyGraphTransformationContext(
                    mvn.getSession());
            try {
                // add conflict IDs to the added deployments
                resolvedDeps = new ConflictMarker().transformGraph(resolvedDeps, context);
                // resolves version conflicts
                resolvedDeps = new ConflictIdSorter().transformGraph(resolvedDeps, context);
                resolvedDeps = mvn.getSession().getDependencyGraphTransformer().transformGraph(resolvedDeps, context);
            } catch (RepositoryException e) {
                throw new AppModelResolverException("Failed to normalize the dependency graph", e);
            }
            final BuildDependencyGraphVisitor buildDepsVisitor = new BuildDependencyGraphVisitor(
                    deploymentInjector.allRuntimeDeps,
                    buildTreeConsumer);
            buildDepsVisitor.visit(resolvedDeps);
            final List<ArtifactRequest> requests = buildDepsVisitor.getArtifactRequests();
            if (!requests.isEmpty()) {
                final List<ArtifactResult> results = mvn.resolve(requests);
                // update the artifacts in the graph
                for (ArtifactResult result : results) {
                    final Artifact artifact = result.getArtifact();
                    if (artifact != null) {
                        result.getRequest().getDependencyNode().setArtifact(artifact);
                    }
                }
                final List<DependencyNode> deploymentDepNodes = buildDepsVisitor.getDeploymentNodes();
                for (DependencyNode dep : deploymentDepNodes) {
                    appBuilder.addDependency(new AppDependency(BootstrapAppModelResolver.toAppArtifact(dep.getArtifact()),
                            dep.getDependency().getScope(), dep.getDependency().isOptional(),
                            AppDependency.DEPLOYMENT_CP_FLAG));
                }
            }
        }

        collectPlatformProperties(appBuilder, managedDeps);

        //we need these to have a type of 'jar'
        //type is blank when loaded
        for (AppArtifactKey i : localProjects) {
            appBuilder.addLocalProjectArtifact(new AppArtifactKey(i.getGroupId(), i.getArtifactId(), null, "jar"));
        }
        return appBuilder.build();
    }

    private void collectPlatformProperties(AppModel.Builder appBuilder, List<Dependency> managedDeps)
            throws AppModelResolverException {
        final PlatformImportsImpl platformReleases = new PlatformImportsImpl();
        for (Dependency d : managedDeps) {
            final Artifact artifact = d.getArtifact();
            final String extension = artifact.getExtension();
            final String artifactId = artifact.getArtifactId();
            if ("json".equals(extension)
                    && artifactId.endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                platformReleases.addPlatformDescriptor(artifact.getGroupId(), artifactId, artifact.getClassifier(), extension,
                        artifact.getVersion());
            } else if ("properties".equals(artifact.getExtension())
                    && artifactId.endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
                platformReleases.addPlatformProperties(artifact.getGroupId(), artifactId, artifact.getClassifier(), extension,
                        artifact.getVersion(), mvn.resolve(artifact).getArtifact().getFile().toPath());
            }
        }
        appBuilder.setPlatformImports(platformReleases);
    }

    @Override
    public List<String> listLaterVersions(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, appArtifact.getVersion(), false,
                upToVersion, inclusive);
        final List<Version> resolvedVersions = rangeResult.getVersions();
        final List<String> versions = new ArrayList<>(resolvedVersions.size());
        for (Version v : resolvedVersions) {
            versions.add(v.toString());
        }
        return versions;
    }

    @Override
    public String getNextVersion(AppArtifact appArtifact, String fromVersion, boolean fromVersionIncluded, String upToVersion,
            boolean upToVersionInclusive) throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, fromVersion, fromVersionIncluded,
                upToVersion, upToVersionInclusive);
        return getEarliest(rangeResult);
    }

    @Override
    public String getLatestVersion(AppArtifact appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, appArtifact.getVersion(), false,
                upToVersion, inclusive);
        final String latest = getLatest(rangeResult);
        return latest == null ? appArtifact.getVersion() : latest;
    }

    @Override
    public String getLatestVersionFromRange(AppArtifact appArtifact, String range) throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, range);
        return getLatest(rangeResult);
    }

    public List<RemoteRepository> resolveArtifactRepos(AppArtifact appArtifact) throws AppModelResolverException {
        return mvn.resolveDescriptor(toAetherArtifact(appArtifact)).getRepositories();
    }

    public void install(AppArtifact appArtifact, Path localPath) throws AppModelResolverException {
        mvn.install(new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(),
                appArtifact.getType(), appArtifact.getVersion(), Collections.emptyMap(), localPath.toFile()));
    }

    private AppArtifactKey getKey(final Artifact artifact) {
        return new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension());
    }

    private String getEarliest(final VersionRangeResult rangeResult) {
        final List<Version> versions = rangeResult.getVersions();
        if (versions.isEmpty()) {
            return null;
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

    private VersionRangeResult resolveVersionRangeResult(AppArtifact appArtifact, String fromVersion,
            boolean fromVersionIncluded, String upToVersion, boolean upToVersionIncluded)
            throws AppModelResolverException {
        return resolveVersionRangeResult(appArtifact,
                (fromVersionIncluded ? '[' : '(')
                        + (fromVersion == null ? "" : fromVersion + ',')
                        + (upToVersion == null ? ')' : upToVersion + (upToVersionIncluded ? ']' : ')')));
    }

    private VersionRangeResult resolveVersionRangeResult(AppArtifact appArtifact, String range)
            throws AppModelResolverException {
        return mvn.resolveVersionRange(new DefaultArtifact(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getType(), range));
    }

    static List<AppDependency> toAppDepList(DependencyNode rootNode) {
        final List<DependencyNode> depNodes = rootNode.getChildren();
        if (depNodes.isEmpty()) {
            return Collections.emptyList();
        }
        final List<AppDependency> appDeps = new ArrayList<>();
        collect(depNodes, appDeps);
        return appDeps;
    }

    private static void collect(List<DependencyNode> nodes, List<AppDependency> appDeps) {
        for (DependencyNode node : nodes) {
            collect(node.getChildren(), appDeps);
            final Dependency dep = node.getDependency();
            appDeps.add(new AppDependency(toAppArtifact(node.getArtifact()), dep.getScope(), dep.isOptional()));
        }
    }

    private static Artifact toAetherArtifact(AppArtifact artifact) {
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getType(), artifact.getVersion());
    }

    private static AppArtifact toAppArtifact(Artifact artifact) {
        final AppArtifact appArtifact = new AppArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        final File file = artifact.getFile();
        if (file != null) {
            appArtifact.setPaths(PathsCollection.of(file.toPath()));
        }
        return appArtifact;
    }

    private static List<Dependency> toAetherDeps(List<AppDependency> directDeps) {
        if (directDeps.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Dependency> directMvnDeps = new ArrayList<>(directDeps.size());
        for (AppDependency dep : directDeps) {
            directMvnDeps.add(new Dependency(toAetherArtifact(dep.getArtifact()), dep.getScope()));
        }
        return directMvnDeps;
    }
}
