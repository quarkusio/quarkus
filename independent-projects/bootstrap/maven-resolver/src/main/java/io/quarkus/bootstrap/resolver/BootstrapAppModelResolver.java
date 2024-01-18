package io.quarkus.bootstrap.resolver;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.toAppArtifact;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.Version;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.resolver.maven.ApplicationDependencyTreeResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvableDependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

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
        return resolve(artifact, toAetherArtifact(artifact), mvn.getRepositories());
    }

    @Override
    public Collection<ResolvedDependency> resolveUserDependencies(ArtifactCoords appArtifact,
            Collection<io.quarkus.maven.dependency.Dependency> deps)
            throws AppModelResolverException {
        final List<Dependency> mvnDeps;
        if (deps.isEmpty()) {
            mvnDeps = List.of();
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
                    result.add(toAppArtifact(dep.getArtifact(), null).setScope(dep.getScope()).setOptional(dep.isOptional())
                            .build());
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
        return resolveManagedModel(appArtifact, List.of(), null, Set.of());
    }

    @Override
    public ApplicationModel resolveModel(ArtifactCoords appArtifact,
            Collection<io.quarkus.maven.dependency.Dependency> directDeps)
            throws AppModelResolverException {
        return resolveManagedModel(appArtifact, directDeps, null, Set.of());
    }

    @Override
    public ApplicationModel resolveManagedModel(ArtifactCoords appArtifact,
            Collection<io.quarkus.maven.dependency.Dependency> directDeps,
            ArtifactCoords managingProject,
            Set<ArtifactKey> reloadableModules)
            throws AppModelResolverException {
        return doResolveModel(appArtifact, toAetherDeps(directDeps), managingProject, reloadableModules);
    }

    /**
     * Resolve application mode for the main application module that might not have a POM file on disk.
     *
     * @param module main application module
     * @return resolved application model
     * @throws AppModelResolverException in case application model could not be resolved
     */
    public ApplicationModel resolveModel(WorkspaceModule module)
            throws AppModelResolverException {
        final PathList.Builder resolvedPaths = PathList.builder();
        if (module.hasMainSources()) {
            if (!module.getMainSources().isOutputAvailable()) {
                throw new AppModelResolverException("The application module hasn't been built yet");
            }
            module.getMainSources().getSourceDirs().forEach(s -> {
                if (!resolvedPaths.contains(s.getOutputDir())) {
                    resolvedPaths.add(s.getOutputDir());
                }
            });
            module.getMainSources().getResourceDirs().forEach(s -> {
                if (!resolvedPaths.contains(s.getOutputDir())) {
                    resolvedPaths.add(s.getOutputDir());
                }
            });
        }
        final Artifact mainArtifact = new DefaultArtifact(module.getId().getGroupId(), module.getId().getArtifactId(), null,
                ArtifactCoords.TYPE_JAR,
                module.getId().getVersion());
        final ResolvedDependency mainDep = ResolvedDependencyBuilder.newInstance()
                .setGroupId(mainArtifact.getGroupId())
                .setArtifactId(mainArtifact.getArtifactId())
                .setClassifier(mainArtifact.getClassifier())
                .setType(mainArtifact.getExtension())
                .setVersion(mainArtifact.getVersion())
                .setResolvedPaths(resolvedPaths.build())
                .setWorkspaceModule(module)
                .build();

        final Map<ArtifactKey, Dependency> managedMap = new HashMap<>();
        for (io.quarkus.maven.dependency.Dependency d : module.getDirectDependencyConstraints()) {
            if (io.quarkus.maven.dependency.Dependency.SCOPE_IMPORT.equals(d.getScope())) {
                mvn.resolveDescriptor(toAetherArtifact(d)).getManagedDependencies()
                        .forEach(dep -> managedMap.putIfAbsent(getKey(dep.getArtifact()), dep));
            } else {
                managedMap.put(d.getKey(), new Dependency(toAetherArtifact(d), d.getScope(), d.isOptional(),
                        toAetherExclusions(d.getExclusions())));
            }
        }
        final List<Dependency> directDeps = new ArrayList<>(module.getDirectDependencies().size());
        for (io.quarkus.maven.dependency.Dependency d : module.getDirectDependencies()) {
            String version = d.getVersion();
            if (version == null) {
                final Dependency constraint = managedMap.get(d.getKey());
                if (constraint == null) {
                    throw new AppModelResolverException(
                            d.toCompactCoords() + " is missing version and is not found among the dependency constraints");
                }
                version = constraint.getArtifact().getVersion();
            }
            directDeps.add(new Dependency(
                    new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), version),
                    d.getScope(), d.isOptional(), toAetherExclusions(d.getExclusions())));
        }
        final List<Dependency> constraints = managedMap.isEmpty() ? List.of() : new ArrayList<>(managedMap.values());

        return buildAppModel(mainDep,
                mainArtifact, directDeps, mvn.getRepositories(),
                Set.of(), constraints);
    }

    private ApplicationModel doResolveModel(ArtifactCoords coords,
            List<Dependency> directMvnDeps,
            ArtifactCoords managingProject,
            Set<ArtifactKey> reloadableModules)
            throws AppModelResolverException {
        if (coords == null) {
            throw new IllegalArgumentException("Application artifact is null");
        }
        Artifact mvnArtifact = toAetherArtifact(coords);

        List<Dependency> managedDeps = List.of();
        List<RemoteRepository> managedRepos = List.of();
        if (managingProject != null) {
            final ArtifactDescriptorResult managingDescr = mvn.resolveDescriptor(toAetherArtifact(managingProject));
            managedDeps = managingDescr.getManagedDependencies();
            managedRepos = mvn.newResolutionRepositories(managingDescr.getRepositories());
        }

        List<RemoteRepository> aggregatedRepos = mvn.aggregateRepositories(managedRepos, mvn.getRepositories());
        final ResolvedDependency appArtifact = resolve(coords, mvnArtifact, aggregatedRepos);
        mvnArtifact = toAetherArtifact(appArtifact);
        final ArtifactDescriptorResult appArtifactDescr = resolveDescriptor(mvnArtifact, aggregatedRepos);

        Map<ArtifactKey, String> managedVersions = Map.of();
        if (!managedDeps.isEmpty()) {
            final List<Dependency> mergedManagedDeps = new ArrayList<>(
                    managedDeps.size() + appArtifactDescr.getManagedDependencies().size());
            managedVersions = new HashMap<>(managedDeps.size());
            for (Dependency dep : managedDeps) {
                managedVersions.put(getKey(dep.getArtifact()), dep.getArtifact().getVersion());
                mergedManagedDeps.add(dep);
            }
            for (Dependency dep : appArtifactDescr.getManagedDependencies()) {
                final ArtifactKey key = getKey(dep.getArtifact());
                if (!managedVersions.containsKey(key)) {
                    mergedManagedDeps.add(dep);
                }
            }
            managedDeps = mergedManagedDeps;
        } else {
            managedDeps = appArtifactDescr.getManagedDependencies();
        }

        directMvnDeps = DependencyUtils.mergeDeps(directMvnDeps, appArtifactDescr.getDependencies(), managedVersions, Set.of());
        aggregatedRepos = mvn.aggregateRepositories(aggregatedRepos,
                mvn.newResolutionRepositories(appArtifactDescr.getRepositories()));

        return buildAppModel(appArtifact,
                mvnArtifact, directMvnDeps, aggregatedRepos,
                reloadableModules, managedDeps);
    }

    private Set<String> getExcludedScopes() {
        if (test) {
            return Set.of();
        }
        if (devmode) {
            return Set.of(JavaScopes.TEST);
        }
        return Set.of(JavaScopes.PROVIDED, JavaScopes.TEST);
    }

    private ApplicationModel buildAppModel(ResolvedDependency appArtifact,
            Artifact artifact, List<Dependency> directDeps, List<RemoteRepository> repos,
            Set<ArtifactKey> reloadableModules, List<Dependency> managedDeps)
            throws AppModelResolverException {

        final ApplicationModelBuilder appBuilder = new ApplicationModelBuilder().setAppArtifact(appArtifact);
        if (appArtifact.getWorkspaceModule() != null) {
            appBuilder.addReloadableWorkspaceModule(appArtifact.getKey());
        }
        if (!reloadableModules.isEmpty()) {
            appBuilder.addReloadableWorkspaceModules(reloadableModules);
        }

        var filteredProvidedDeps = new ArrayList<Dependency>(0);
        var excludedScopes = getExcludedScopes();
        if (!excludedScopes.isEmpty()) {
            var filtered = new ArrayList<Dependency>(directDeps.size());
            for (var d : directDeps) {
                if (!excludedScopes.contains(d.getScope())) {
                    filtered.add(d);
                } else if (JavaScopes.PROVIDED.equals(d.getScope())) {
                    filteredProvidedDeps.add(d);
                }
            }
            directDeps = filtered;
        }
        var collectRtDepsRequest = MavenArtifactResolver.newCollectRequest(artifact, directDeps, managedDeps, List.of(), repos);
        try {
            ApplicationDependencyTreeResolver.newInstance()
                    .setArtifactResolver(mvn)
                    .setApplicationModelBuilder(appBuilder)
                    .setCollectReloadableModules(collectReloadableDeps && reloadableModules.isEmpty())
                    .setCollectCompileOnly(filteredProvidedDeps)
                    .setBuildTreeConsumer(buildTreeConsumer)
                    .resolve(collectRtDepsRequest);
        } catch (BootstrapDependencyProcessingException e) {
            throw new AppModelResolverException(
                    "Failed to inject extension deployment dependencies for " + appArtifact.toCompactCoords(), e);
        }

        return appBuilder.build();
    }

    private io.quarkus.maven.dependency.ResolvedDependency resolve(ArtifactCoords appArtifact, Artifact mvnArtifact,
            List<RemoteRepository> aggregatedRepos) throws BootstrapMavenException {

        final ResolvedDependency resolvedArtifact = ResolvedDependency.class.isAssignableFrom(appArtifact.getClass())
                ? (ResolvedDependency) appArtifact
                : null;
        if (resolvedArtifact != null
                && (resolvedArtifact.getWorkspaceModule() != null || mvn.getProjectModuleResolver() == null)) {
            return resolvedArtifact;
        }

        final WorkspaceModule resolvedModule = mvn.getProjectModuleResolver() == null ? null
                : mvn.getProjectModuleResolver().getProjectModule(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                        appArtifact.getVersion());
        if (resolvedArtifact != null && resolvedModule == null) {
            return resolvedArtifact;
        }

        PathCollection resolvedPaths = null;
        if ((devmode || test) && resolvedModule != null) {
            final ArtifactSources artifactSources = resolvedModule.getSources(appArtifact.getClassifier());
            if (artifactSources != null) {
                final PathList.Builder pathBuilder = PathList.builder();
                collectSourceDirs(pathBuilder, artifactSources.getSourceDirs());
                collectSourceDirs(pathBuilder, artifactSources.getResourceDirs());
                if (!pathBuilder.isEmpty()) {
                    resolvedPaths = pathBuilder.build();
                }
            }
        }
        if (resolvedPaths == null) {
            if (resolvedArtifact == null || resolvedArtifact.getResolvedPaths() == null) {
                resolvedPaths = PathList.of(resolve(mvnArtifact, aggregatedRepos).getArtifact().getFile().toPath());
            } else {
                resolvedPaths = resolvedArtifact.getResolvedPaths();
            }
        }
        return ResolvedDependencyBuilder.newInstance().setCoords(appArtifact).setWorkspaceModule(resolvedModule)
                .setResolvedPaths(resolvedPaths).build();
    }

    private static void collectSourceDirs(final PathList.Builder pathBuilder, Collection<SourceDir> resources) {
        for (SourceDir src : resources) {
            if (Files.exists(src.getOutputDir())) {
                final Path p = src.getOutputDir();
                if (!pathBuilder.contains(p)) {
                    pathBuilder.add(p);
                }
            }
        }
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
                artifact.getVersion(), Map.of(), localPath.toFile()));
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

    private static List<Dependency> toAetherDeps(Collection<io.quarkus.maven.dependency.Dependency> directDeps) {
        if (directDeps.isEmpty()) {
            return List.of();
        }
        final List<Dependency> directMvnDeps = new ArrayList<>(directDeps.size());
        for (io.quarkus.maven.dependency.Dependency dep : directDeps) {
            directMvnDeps.add(new Dependency(toAetherArtifact(dep), dep.getScope()));
        }
        return directMvnDeps;
    }

    private static List<Exclusion> toAetherExclusions(Collection<ArtifactKey> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<Exclusion>(keys.size());
        for (ArtifactKey key : keys) {
            result.add(new Exclusion(key.getGroupId(), key.getArtifactId(), key.getClassifier(),
                    key.getType() == null || key.getType().isBlank() ? ArtifactCoords.TYPE_JAR : key.getType()));
        }
        return result;
    }

    private ArtifactResult resolve(Artifact artifact, List<RemoteRepository> aggregatedRepos)
            throws BootstrapMavenException {
        try {
            return mvn.getSystem().resolveArtifact(mvn.getSession(),
                    new ArtifactRequest()
                            .setArtifact(artifact)
                            .setRepositories(aggregatedRepos));
        } catch (ArtifactResolutionException e) {
            throw new BootstrapMavenException("Failed to resolve artifact " + artifact, e);
        }
    }

    private ArtifactDescriptorResult resolveDescriptor(Artifact artifact, List<RemoteRepository> aggregatedRepos)
            throws BootstrapMavenException {
        try {
            return mvn.getSystem().readArtifactDescriptor(mvn.getSession(),
                    new ArtifactDescriptorRequest()
                            .setArtifact(artifact)
                            .setRepositories(aggregatedRepos));
        } catch (ArtifactDescriptorException e) {
            throw new BootstrapMavenException("Failed to read descriptor of " + artifact, e);
        }
    }
}
