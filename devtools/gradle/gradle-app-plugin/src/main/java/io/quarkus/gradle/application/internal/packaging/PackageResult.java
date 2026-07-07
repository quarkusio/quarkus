package io.quarkus.gradle.application.internal.packaging;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public record PackageResult(
        String buildName,
        QuarkusApplicationBuildType buildType,
        Path outputRoot,
        String outputName,
        Path jarPath,
        Optional<Path> originalArtifact,
        Optional<Path> libraryDirectory,
        boolean mutable,
        boolean uberJar,
        Optional<String> classifier,
        List<Artifact> artifacts) {

    public static final String SCHEMA_VERSION = "1";

    public PackageResult {
        if (buildName == null || buildName.isBlank()) {
            throw new IllegalArgumentException("Package result build name must not be empty");
        }
        requireNonNull(buildType, "buildType");
        requireNonNull(outputRoot, "outputRoot");
        if (outputName == null || outputName.isBlank()) {
            throw new IllegalArgumentException("Package result output name must not be empty");
        }
        requireNonNull(jarPath, "jarPath");
        requireNonNull(originalArtifact, "originalArtifact");
        requireNonNull(libraryDirectory, "libraryDirectory");
        requireNonNull(classifier, "classifier");
        artifacts = List.copyOf(requireNonNull(artifacts, "artifacts"));
    }

    public record Artifact(Optional<Path> path, String type, Map<String, String> metadata) {
        public Artifact {
            requireNonNull(path, "path");
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Package result artifact type must not be empty");
            }
            metadata = Map.copyOf(requireNonNull(metadata, "metadata"));
        }
    }
}
