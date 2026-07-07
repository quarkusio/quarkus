package io.quarkus.gradle.model.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;

import io.quarkus.maven.dependency.GAV;

/**
 * Resolves POMs from a previously generated closure and explicitly supplied local repository roots.
 * <p>
 * Resolution never contacts a repository service. It first uses the resolved/missing results supplied at construction,
 * then looks for Maven-layout POMs and unambiguous Gradle-cache POMs under the known roots. It can also derive a
 * Maven-layout repository root from a known POM path. This makes it suitable for configuration-cache-compatible task
 * execution and offline model generation.
 * <p>
 * A parent or imported BOM is available only when it is present in those inputs or local roots; this resolver does not
 * independently discover or download Maven metadata.
 */
public final class KnownPomResolver implements PomResolver {

    private final Map<GAV, Optional<File>> pomCache = new ConcurrentHashMap<>();
    private final Map<GAV, File> resolvedPomFiles;
    private final ArrayList<File> repositoryRoots;

    /**
     * Creates a local-only resolver.
     *
     * @param resolvedPomFiles known POM coordinates and files
     * @param missingPoms coordinates already known to be unavailable
     * @param repositoryRoots local Maven repositories or Gradle artifact-cache roots to search
     */
    public KnownPomResolver(Map<GAV, File> resolvedPomFiles, Collection<GAV> missingPoms,
            Collection<File> repositoryRoots) {
        this.resolvedPomFiles = Map.copyOf(resolvedPomFiles);
        this.repositoryRoots = new ArrayList<>(repositoryRoots);
        resolvedPomFiles.forEach((gav, file) -> pomCache.put(gav, Optional.of(file)));
        missingPoms.forEach(gav -> pomCache.putIfAbsent(gav, Optional.empty()));
    }

    /**
     * Named factory equivalent to {@link #KnownPomResolver(Map, Collection, Collection)}.
     *
     * @param resolvedPomFiles known POM coordinates and files
     * @param missingPoms coordinates already known to be unavailable
     * @param repositoryRoots local Maven repositories or Gradle artifact-cache roots to search
     * @return a local-only resolver
     */
    public static KnownPomResolver fromPomClosure(Map<GAV, File> resolvedPomFiles, Collection<GAV> missingPoms,
            Collection<File> repositoryRoots) {
        return new KnownPomResolver(resolvedPomFiles, missingPoms, repositoryRoots);
    }

    /**
     * Populates resolved or known-missing results for uncached coordinates using only known local repositories.
     *
     * @param gavs POM coordinates to look up
     */
    @Override
    public void prefetchPoms(Collection<GAV> gavs) {
        gavs.stream()
                .filter(gav -> !pomCache.containsKey(gav))
                .forEach(gav -> pomCache.putIfAbsent(gav, resolvePomFromKnownRepositories(gav)));
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
     * Resolves a POM from the local cache/search roots.
     *
     * @param gav POM coordinates to resolve
     * @return Maven model source backed by the resolved local file
     * @throws UnresolvableModelException when no unambiguous local POM can be found
     */
    @Override
    public ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException {
        File pomFile = pomCache.computeIfAbsent(gav, this::resolvePomFromKnownRepositories).orElse(null);
        if (pomFile == null) {
            throw new UnresolvableModelException(
                    "Could not resolve POM for " + gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion(),
                    gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
        }
        return new FileModelSource(pomFile);
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
