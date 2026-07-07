package io.quarkus.gradle.application.internal.nativeimage;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public record NativeResult(
        String buildName,
        QuarkusApplicationBuildType buildType,
        Path outputRoot,
        String outputName,
        Optional<Path> executablePath,
        Optional<Path> sourcesDirectory,
        Optional<Path> sourceJarPath,
        Optional<Path> nativeImageArgsPath,
        Map<String, String> graalVMInfo,
        List<Artifact> artifacts) {

    public static final String SCHEMA_VERSION = "1";

    public NativeResult {
        if (buildName == null || buildName.isBlank()) {
            throw new IllegalArgumentException("Native result build name must not be empty");
        }
        requireNonNull(buildType, "buildType");
        if (!buildType.isNativeOutput()) {
            throw new IllegalArgumentException("Native result build type must be native: " + buildType);
        }
        requireNonNull(outputRoot, "outputRoot");
        if (outputName == null || outputName.isBlank()) {
            throw new IllegalArgumentException("Native result output name must not be empty");
        }
        requireNonNull(executablePath, "executablePath");
        requireNonNull(sourcesDirectory, "sourcesDirectory");
        requireNonNull(sourceJarPath, "sourceJarPath");
        requireNonNull(nativeImageArgsPath, "nativeImageArgsPath");
        graalVMInfo = Map.copyOf(requireNonNull(graalVMInfo, "graalVMInfo"));
        artifacts = List.copyOf(requireNonNull(artifacts, "artifacts"));
    }

    public String resultType() {
        return buildType.isNativeSources() ? "native-sources" : "native-executable";
    }

    public record Artifact(Optional<Path> path, String type, Map<String, String> metadata) {
        public Artifact {
            requireNonNull(path, "path");
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Native result artifact type must not be empty");
            }
            metadata = Map.copyOf(requireNonNull(metadata, "metadata"));
        }
    }
}
