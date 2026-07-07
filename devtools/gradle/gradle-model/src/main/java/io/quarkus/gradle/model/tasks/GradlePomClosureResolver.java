package io.quarkus.gradle.model.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;

import io.quarkus.gradle.model.pom.PomResolver;
import io.quarkus.maven.dependency.GAV;

/**
 * POM resolver backed by Gradle artifact-resolution queries with a local-repository fallback.
 * <p>
 * This resolver is used while Gradle services are available to prepare a closure of lookup outcomes for later task
 * execution.
 * Maven effective-model construction may reveal parent and imported-BOM coordinates; batch prefetch records both
 * resolved and missing results so callers can retry without repeatedly resolving the same POM.
 * <p>
 * Passing no dependency handler disables Gradle queries and restricts resolution to supplied POMs and local repository
 * roots. The resolver ignores Maven-declared repositories: Gradle's configured repositories remain authoritative.
 */
public class GradlePomClosureResolver implements PomResolver {

    private final DependencyHandler dependencies;
    private final Map<GAV, Optional<File>> pomCache = new ConcurrentHashMap<>();
    private final Map<GAV, File> resolvedPomFiles;
    private final List<File> repositoryRoots;

    /**
     * Creates a resolver that starts from modeled POM files but falls back to Gradle artifact resolution for missing POMs.
     * This is intended for legacy/tooling compatibility and dedicated POM-closure producer tasks.
     *
     * @param resolvedPomFiles POM artifacts already resolved by a Gradle artifact view
     * @param dependencies Gradle dependency handler used for artifact-resolution queries
     * @param repositoryRoots local Maven repositories or Gradle artifact-cache roots used as fallback
     */
    public static GradlePomClosureResolver withGradleArtifactResolution(Map<GAV, File> resolvedPomFiles,
            DependencyHandler dependencies, Collection<File> repositoryRoots) {
        return new GradlePomClosureResolver(resolvedPomFiles, dependencies, repositoryRoots);
    }

    private GradlePomClosureResolver(Map<GAV, File> resolvedPomFiles, DependencyHandler dependencies,
            Collection<File> repositoryRoots) {
        this.dependencies = dependencies;
        this.resolvedPomFiles = Map.copyOf(resolvedPomFiles);
        this.repositoryRoots = new ArrayList<>(repositoryRoots);
        resolvedPomFiles.forEach((gav, file) -> pomCache.put(gav, Optional.of(file)));
    }

    /**
     * Returns a snapshot of every resolved or known-missing lookup attempted by this resolver.
     *
     * @return an immutable snapshot of the lookup cache
     */
    public Map<GAV, Optional<File>> getPomResults() {
        return Map.copyOf(pomCache);
    }

    /**
     * Prefetches POM artifacts for Gradle module identifiers, using one artifact-resolution query when possible.
     *
     * @param moduleIds module identifiers to consume
     */
    public void prefetchPoms(Stream<ModuleComponentIdentifier> moduleIds) {
        Set<ModuleComponentIdentifier> unresolvedModuleIds = new HashSet<>();
        moduleIds.filter(moduleId -> !pomCache.containsKey(toGav(moduleId)))
                .forEach(unresolvedModuleIds::add);
        if (unresolvedModuleIds.isEmpty()) {
            return;
        }
        if (dependencies == null) {
            unresolvedModuleIds.stream()
                    .map(GradlePomClosureResolver::toGav)
                    .forEach(gav -> pomCache.putIfAbsent(gav, resolvePomFromKnownRepositories(gav)));
            return;
        }

        @SuppressWarnings("unchecked")
        var result = dependencies.createArtifactResolutionQuery()
                .forComponents(unresolvedModuleIds)
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute();

        Set<GAV> unresolvedGavs = new HashSet<>();
        unresolvedModuleIds.stream()
                .map(GradlePomClosureResolver::toGav)
                .forEach(unresolvedGavs::add);
        for (var component : result.getResolvedComponents()) {
            ComponentIdentifier componentId = component.getId();
            if (componentId instanceof ModuleComponentIdentifier moduleId) {
                Optional<File> pom = Optional.empty();
                for (var artifactResult : component.getArtifacts(MavenPomArtifact.class)) {
                    if (artifactResult instanceof ResolvedArtifactResult resolved) {
                        pom = Optional.of(resolved.getFile());
                        break;
                    }
                }
                var gav = toGav(moduleId);
                pomCache.putIfAbsent(gav, pom);
                unresolvedGavs.remove(gav);
            }
        }

        for (var unresolvedGav : unresolvedGavs) {
            pomCache.putIfAbsent(unresolvedGav, Optional.empty());
        }
    }

    /**
     * Prefetches POM artifacts for coordinates and caches resolved or missing outcomes.
     *
     * @param gavs POM coordinates to look up
     */
    @Override
    public void prefetchPoms(Collection<GAV> gavs) {
        Set<GAV> unresolvedGavs = new HashSet<>();
        gavs.stream()
                .filter(gav -> !pomCache.containsKey(gav))
                .forEach(unresolvedGavs::add);
        if (unresolvedGavs.isEmpty()) {
            return;
        }

        for (GAV gav : unresolvedGavs) {
            pomCache.putIfAbsent(gav, dependencies == null ? resolvePomFromKnownRepositories(gav) : resolvePomViaQuery(gav));
        }
    }

    /**
     * @param gav POM coordinates to query
     * @return whether a resolved or known-missing result is cached for {@code gav}
     */
    @Override
    public boolean hasPomResult(GAV gav) {
        return pomCache.containsKey(gav);
    }

    /**
     * Resolves a POM, querying Gradle or local roots on the first lookup.
     *
     * @param gav POM coordinates to resolve
     * @return Maven model source backed by the resolved POM file
     * @throws UnresolvableModelException when the POM cannot be found
     */
    @Override
    public ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException {
        File pomFile = pomCache
                .computeIfAbsent(gav, this::resolvePomViaQuery)
                .orElse(null);

        if (pomFile == null) {
            throw new UnresolvableModelException(
                    "Could not resolve POM for " + gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion(),
                    gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
        }

        return new FileModelSource(pomFile);
    }

    private Optional<File> resolvePomViaQuery(GAV gav) {
        if (dependencies == null) {
            return resolvePomFromKnownRepositories(gav);
        }
        @SuppressWarnings("unchecked")
        var componentId = dependencies.createArtifactResolutionQuery()
                .forModule(gav.getGroupId(), gav.getArtifactId(), gav.getVersion())
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute();

        for (var component : componentId.getResolvedComponents()) {
            for (var artifactResult : component.getArtifacts(MavenPomArtifact.class)) {
                if (artifactResult instanceof ResolvedArtifactResult resolved) {
                    return Optional.of(resolved.getFile());
                }
            }
        }
        return resolvePomFromKnownRepositories(gav);
    }

    private Optional<File> resolvePomFromKnownRepositories(GAV gav) {
        File resolved = resolvedPomFiles.get(gav);
        if (resolved != null) {
            return Optional.of(resolved);
        }

        String targetSuffix = repositoryPomPath(gav);
        for (File repositoryRoot : repositoryRoots) {
            Path candidate = repositoryRoot.toPath().resolve(targetSuffix);
            if (candidate.toFile().isFile()) {
                return Optional.of(candidate.toFile());
            }
            Optional<File> gradleCachePom = findGradleCachePom(repositoryRoot.toPath(), gav);
            if (gradleCachePom.isPresent()) {
                return gradleCachePom;
            }
        }

        for (var entry : resolvedPomFiles.entrySet()) {
            Path knownPom = entry.getValue().toPath();
            String knownSuffix = repositoryPomPath(entry.getKey());
            if (!knownPom.endsWith(knownSuffix)) {
                continue;
            }
            Path repositoryRoot = knownPom;
            for (int i = 0; i < Path.of(knownSuffix).getNameCount(); i++) {
                repositoryRoot = repositoryRoot.getParent();
            }
            Path candidate = repositoryRoot.resolve(targetSuffix);
            if (candidate.toFile().isFile()) {
                return Optional.of(candidate.toFile());
            }
        }
        return Optional.empty();
    }

    private static Optional<File> findGradleCachePom(Path repositoryRoot, GAV gav) {
        Path moduleDirectory = repositoryRoot
                .resolve(gav.getGroupId())
                .resolve(gav.getArtifactId())
                .resolve(gav.getVersion());
        if (!moduleDirectory.toFile().isDirectory()) {
            return Optional.empty();
        }
        File[] hashDirectories = moduleDirectory.toFile().listFiles(File::isDirectory);
        if (hashDirectories == null) {
            return Optional.empty();
        }
        String pomFileName = gav.getArtifactId() + "-" + gav.getVersion() + ".pom";
        File resolvedPom = null;
        for (File hashDirectory : hashDirectories) {
            File candidate = hashDirectory.toPath().resolve(pomFileName).toFile();
            if (candidate.isFile()) {
                if (resolvedPom != null) {
                    return Optional.empty();
                }
                resolvedPom = candidate;
            }
        }
        return Optional.ofNullable(resolvedPom);
    }

    private static String repositoryPomPath(GAV gav) {
        return gav.getGroupId().replace('.', File.separatorChar)
                + File.separator + gav.getArtifactId()
                + File.separator + gav.getVersion()
                + File.separator + gav.getArtifactId() + "-" + gav.getVersion() + ".pom";
    }

    private static GAV toGav(ModuleComponentIdentifier moduleId) {
        return new GAV(moduleId.getGroup(), moduleId.getModule(), moduleId.getVersion());
    }

    private record FileModelSource(File file) implements ModelSource2 {

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public String getLocation() {
            return file.getAbsolutePath();
        }

        @Override
        public ModelSource2 getRelatedSource(String relPath) {
            return null;
        }

        @Override
        public URI getLocationURI() {
            return file.toURI();
        }
    }
}
