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

public final class KnownPomResolver implements PomResolver {

    private final Map<GAV, Optional<File>> pomCache = new ConcurrentHashMap<>();
    private final Map<GAV, File> resolvedPomFiles;
    private final ArrayList<File> repositoryRoots;

    public KnownPomResolver(Map<GAV, File> resolvedPomFiles, Collection<GAV> missingPoms,
            Collection<File> repositoryRoots) {
        this.resolvedPomFiles = Map.copyOf(resolvedPomFiles);
        this.repositoryRoots = new ArrayList<>(repositoryRoots);
        resolvedPomFiles.forEach((gav, file) -> pomCache.put(gav, Optional.of(file)));
        missingPoms.forEach(gav -> pomCache.putIfAbsent(gav, Optional.empty()));
    }

    public static KnownPomResolver fromPomClosure(Map<GAV, File> resolvedPomFiles, Collection<GAV> missingPoms,
            Collection<File> repositoryRoots) {
        return new KnownPomResolver(resolvedPomFiles, missingPoms, repositoryRoots);
    }

    @Override
    public void prefetchPoms(Collection<GAV> gavs) {
        gavs.stream()
                .filter(gav -> !pomCache.containsKey(gav))
                .forEach(gav -> pomCache.putIfAbsent(gav, resolvePomFromKnownRepositories(gav)));
    }

    @Override
    public boolean hasPomResult(GAV gav) {
        return pomCache.containsKey(gav);
    }

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
