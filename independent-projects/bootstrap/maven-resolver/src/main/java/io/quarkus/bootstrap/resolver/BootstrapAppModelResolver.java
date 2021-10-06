package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.BuildDependencyGraphVisitor;
import io.quarkus.bootstrap.resolver.maven.DeploymentInjectingDependencyVisitor;
import io.quarkus.bootstrap.resolver.maven.DeploymentInjectionException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.SimpleDependencyGraphTransformationContext;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.bootstrap.workspace.ProcessedSources;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvableDependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
    private boolean collectReloadableDeps = true;

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

    public BootstrapAppModelResolver setCollectReloadableDependencies(boolean collectReloadableDeps) {
        this.collectReloadableDeps = collectReloadableDeps;
        return this;
    }

    public void addRemoteRepositories(List<RemoteRepository> repos) {
        mvn.addRemoteRepositories(repos);
    }

    @Override
    public void relink(ArtifactCoords artifact, Path path) throws AppModelResolverException {
        if (mvn.getLocalRepositoryManager() == null) {
            return;
        }
        mvn.getLocalRepositoryManager().relink(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getType(), artifact.getVersion(), path);
        if (artifact instanceof ResolvableDependency) {
            ((ResolvableDependency) artifact).setResolvedPaths(PathList.of(path));
        }
    }

    @Override
    public ResolvedDependency resolve(ArtifactCoords artifact) throws AppModelResolverException {
        return resolve(artifact, toAetherArtifact(artifact), Collections.emptyList());
    }

    @Override
    public Collection<ResolvedDependency> resolveUserDependencies(ArtifactCoords appArtifact,
            Collection<io.quarkus.maven.dependency.Dependency> deps)
            throws AppModelResolverException {
        final List<Dependency> mvnDeps;
        if (deps.isEmpty()) {
            mvnDeps = Collections.emptyList();
        } else {
            mvnDeps = new ArrayList<>(deps.size());
            for (io.quarkus.maven.dependency.Dependency dep : deps) {
                mvnDeps.add(new Dependency(toAetherArtifact(dep), dep.getScope()));
            }
        }
        final List<ResolvedDependency> result = new ArrayList<>();
        final TreeDependencyVisitor visitor = new TreeDependencyVisitor(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                final Dependency dep = node.getDependency();
                if (dep != null) {
                    result.add(toAppArtifact(dep.getArtifact()).setScope(dep.getScope()).setOptional(dep.isOptional()).build());
                }
                return true;
            }
        });
        mvn.resolveDependencies(toAetherArtifact(appArtifact), mvnDeps).getRoot().accept(visitor);
        return result;
    }

    @Override
    public ApplicationModel resolveModel(ArtifactCoords appArtifact)
            throws AppModelResolverException {
        return resolveManagedModel(appArtifact, Collections.emptyList(), null, Collections.emptySet());
    }

    @Override
    public ApplicationModel resolveModel(ArtifactCoords appArtifact,
            Collection<io.quarkus.maven.dependency.Dependency> directDeps)
            throws AppModelResolverException {
        return resolveManagedModel(appArtifact, directDeps,
                null, Collections.emptySet());
    }

    @Override
    public ApplicationModel resolveManagedModel(ArtifactCoords appArtifact,
            Collection<io.quarkus.maven.dependency.Dependency> directDeps,
            ArtifactCoords managingProject,
            Set<ArtifactKey> reloadableModules)
            throws AppModelResolverException {
        return doResolveModel(appArtifact, toAetherDeps(directDeps), managingProject, reloadableModules);
    }

    private ApplicationModel doResolveModel(ArtifactCoords coords,
            List<Dependency> directMvnDeps,
            ArtifactCoords managingProject,
            Set<ArtifactKey> reloadableModules)
            throws AppModelResolverException {
        if (coords == null) {
            throw new IllegalArgumentException("Application artifact is null");
        }
        final Artifact mvnArtifact = toAetherArtifact(coords);

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

        final ResolvedDependency appArtifact = resolve(coords, mvnArtifact, managedRepos);
        final boolean preferWorkspacePaths = !containsExtensionMetadata(appArtifact) && (devmode || test);

        final ApplicationModelBuilder appBuilder = new ApplicationModelBuilder().setAppArtifact(appArtifact);
        if (appArtifact.getWorkspaceModule() != null) {
            appBuilder.addReloadableWorkspaceModule(new GACT(appArtifact.getGroupId(), appArtifact.getArtifactId()));
        }
        if (!reloadableModules.isEmpty()) {
            appBuilder.addReloadableWorkspaceModules(reloadableModules);
        }

        DependencyNode resolvedDeps = mvn.resolveManagedDependencies(mvnArtifact,
                directMvnDeps, managedDeps, managedRepos, excludedScopes.toArray(new String[0])).getRoot();

        ArtifactDescriptorResult appArtifactDescr = mvn.resolveDescriptor(toAetherArtifact(appArtifact));
        if (managingProject == null) {
            managedDeps = appArtifactDescr.getManagedDependencies();
        } else {
            final List<Dependency> mergedManagedDeps = new ArrayList<>(managedDeps.size());
            final Set<ArtifactKey> mergedKeys = new HashSet<>(managedDeps.size());
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
            deploymentInjector = new DeploymentInjectingDependencyVisitor(mvn, managedDeps, repos, appBuilder,
                    preferWorkspacePaths,
                    collectReloadableDeps && reloadableModules.isEmpty());
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
                    int flags = DependencyFlags.DEPLOYMENT_CP;
                    if (dep.getDependency().isOptional()) {
                        flags |= DependencyFlags.OPTIONAL;
                    }
                    WorkspaceModule module = null;
                    if (mvn.getProjectModuleResolver() != null) {
                        module = mvn.getProjectModuleResolver().getProjectModule(dep.getArtifact().getGroupId(),
                                dep.getArtifact().getArtifactId());
                        if (module != null) {
                            flags |= DependencyFlags.WORKSPACE_MODULE;
                        }
                    }
                    appBuilder.addDependency(
                            toAppArtifact(dep.getArtifact(), module, false)
                                    .setScope(dep.getDependency().getScope())
                                    .setFlags(flags).build());
                }
            }
        }

        collectPlatformProperties(appBuilder, managedDeps);

        return appBuilder.build();
    }

    private static boolean containsExtensionMetadata(ResolvedDependency dep) {
        if (!ArtifactCoords.TYPE_JAR.equals(dep.getType())) {
            return false;
        }
        for (Path path : dep.getResolvedPaths()) {
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                if (containsExtensionMetadata(path)) {
                    return true;
                }
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                    if (containsExtensionMetadata(artifactFs.getPath(""))) {
                        return true;
                    }
                } catch (IOException e) {
                    throw new DeploymentInjectionException("Failed to read " + path, e);
                }
            }
        }
        return false;
    }

    private static boolean containsExtensionMetadata(final Path path) {
        return Files.exists(path.resolve(BootstrapConstants.BUILD_STEPS_PATH))
                || Files.exists(path.resolve(BootstrapConstants.DESCRIPTOR_PATH));
    }

    private io.quarkus.maven.dependency.ResolvedDependency resolve(ArtifactCoords appArtifact, Artifact mvnArtifact,
            List<RemoteRepository> managedRepos) throws BootstrapMavenException {

        final ResolvedDependency resolvedArtifact = ResolvedDependency.class.isAssignableFrom(appArtifact.getClass())
                ? (ResolvedDependency) appArtifact
                : null;
        if (resolvedArtifact != null
                && (resolvedArtifact.getWorkspaceModule() != null || mvn.getProjectModuleResolver() == null)) {
            return resolvedArtifact;
        }

        final WorkspaceModule resolvedModule = mvn.getProjectModuleResolver() == null ? null
                : mvn.getProjectModuleResolver().getProjectModule(appArtifact.getGroupId(), appArtifact.getArtifactId());
        if (resolvedArtifact != null && resolvedModule == null) {
            return resolvedArtifact;
        }

        PathCollection resolvedPaths = null;
        if ((devmode || test) && resolvedModule != null) {
            final PathList.Builder pathBuilder = PathList.builder();
            for (ProcessedSources src : resolvedModule.getMainSources()) {
                if (src.getDestinationDir().exists()) {
                    final Path p = src.getDestinationDir().toPath();
                    if (!pathBuilder.contains(p)) {
                        pathBuilder.add(p);
                    }
                }
            }
            for (ProcessedSources src : resolvedModule.getMainResources()) {
                if (src.getDestinationDir().exists()) {
                    final Path p = src.getDestinationDir().toPath();
                    if (!pathBuilder.contains(p)) {
                        pathBuilder.add(p);
                    }
                }
            }
            if (!pathBuilder.isEmpty()) {
                resolvedPaths = pathBuilder.build();
            }
        }
        if (resolvedPaths == null) {
            if (resolvedArtifact == null || resolvedArtifact.getResolvedPaths() == null) {
                resolvedPaths = PathList.of(mvn.resolve(mvnArtifact, managedRepos).getArtifact().getFile().toPath());
            } else {
                resolvedPaths = resolvedArtifact.getResolvedPaths();
            }
        }
        return ResolvedDependencyBuilder.newInstance().setCoords(appArtifact).setWorkspaceModule(resolvedModule)
                .setResolvedPaths(resolvedPaths).build();
    }

    private void collectPlatformProperties(ApplicationModelBuilder appBuilder, List<Dependency> managedDeps)
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
    public List<String> listLaterVersions(ArtifactCoords appArtifact, String upToVersion, boolean inclusive)
            throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, appArtifact.getVersion(),
                false,
                upToVersion, inclusive);
        final List<Version> resolvedVersions = rangeResult.getVersions();
        final List<String> versions = new ArrayList<>(resolvedVersions.size());
        for (Version v : resolvedVersions) {
            versions.add(v.toString());
        }
        return versions;
    }

    @Override
    public String getNextVersion(ArtifactCoords appArtifact, String fromVersion,
            boolean fromVersionIncluded, String upToVersion,
            boolean upToVersionInclusive) throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, fromVersion, fromVersionIncluded,
                upToVersion, upToVersionInclusive);
        return getEarliest(rangeResult);
    }

    @Override
    public String getLatestVersion(ArtifactCoords appArtifact, String upToVersion,
            boolean inclusive)
            throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, appArtifact.getVersion(),
                false,
                upToVersion, inclusive);
        final String latest = getLatest(rangeResult);
        return latest == null ? appArtifact.getVersion() : latest;
    }

    @Override
    public String getLatestVersionFromRange(ArtifactCoords appArtifact, String range)
            throws AppModelResolverException {
        final VersionRangeResult rangeResult = resolveVersionRangeResult(appArtifact, range);
        return getLatest(rangeResult);
    }

    public List<RemoteRepository> resolveArtifactRepos(ArtifactCoords appArtifact) throws AppModelResolverException {
        return mvn.resolveDescriptor(toAetherArtifact(appArtifact)).getRepositories();
    }

    public void install(ArtifactCoords artifact, Path localPath)
            throws AppModelResolverException {
        mvn.install(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getType(),
                artifact.getVersion(), Collections.emptyMap(), localPath.toFile()));
    }

    private static ArtifactKey getKey(Artifact artifact) {
        return DeploymentInjectingDependencyVisitor.getKey(artifact);
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

    private VersionRangeResult resolveVersionRangeResult(ArtifactCoords appArtifact,
            String fromVersion,
            boolean fromVersionIncluded, String upToVersion, boolean upToVersionIncluded)
            throws AppModelResolverException {
        return resolveVersionRangeResult(appArtifact,
                (fromVersionIncluded ? '[' : '(')
                        + (fromVersion == null ? "" : fromVersion + ',')
                        + (upToVersion == null ? ')' : upToVersion + (upToVersionIncluded ? ']' : ')')));
    }

    private VersionRangeResult resolveVersionRangeResult(ArtifactCoords appArtifact, String range)
            throws AppModelResolverException {
        return mvn.resolveVersionRange(new DefaultArtifact(appArtifact.getGroupId(),
                appArtifact.getArtifactId(), appArtifact.getType(), range));
    }

    private static Artifact toAetherArtifact(ArtifactCoords artifact) {
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getType(), artifact.getVersion());
    }

    private ResolvedDependencyBuilder toAppArtifact(Artifact artifact) {
        return toAppArtifact(artifact, null, false);
    }

    private ResolvedDependencyBuilder toAppArtifact(Artifact artifact, WorkspaceModule module, boolean preferWorkspacePaths) {
        return DeploymentInjectingDependencyVisitor.toAppArtifact(artifact, module, preferWorkspacePaths);
    }

    private static List<Dependency> toAetherDeps(Collection<io.quarkus.maven.dependency.Dependency> directDeps) {
        if (directDeps.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Dependency> directMvnDeps = new ArrayList<>(directDeps.size());
        for (io.quarkus.maven.dependency.Dependency dep : directDeps) {
            directMvnDeps.add(new Dependency(toAetherArtifact(dep), dep.getScope()));
        }
        return directMvnDeps;
    }
}
