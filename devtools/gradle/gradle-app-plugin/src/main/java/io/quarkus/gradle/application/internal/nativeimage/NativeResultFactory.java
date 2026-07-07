package io.quarkus.gradle.application.internal.nativeimage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.GradleException;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public final class NativeResultFactory {

    private static final String NATIVE = "native";
    private static final String NATIVE_SOURCES = "native-sources";

    public NativeResult fromAugmentResult(BuildRequest request, AugmentResult result) {
        if (result == null) {
            throw new GradleException("Quarkus native build for '" + request.descriptor().name()
                    + "' did not produce an augmentation result");
        }
        QuarkusApplicationBuildType type = request.descriptor().type();
        if (type == QuarkusApplicationBuildType.NATIVE_SOURCES) {
            return nativeSources(request, result);
        }
        if (type == QuarkusApplicationBuildType.NATIVE_EXECUTABLE) {
            return nativeExecutable(request, result);
        }
        throw new GradleException("Quarkus native result requested for non-native output '"
                + request.descriptor().name() + "' of type " + type);
    }

    private static NativeResult nativeExecutable(BuildRequest request,
            AugmentResult result) {
        Optional<Path> executable = Optional.ofNullable(result.getNativeResult())
                .or(() -> artifactPath(result, NATIVE));
        if (executable.isEmpty()) {
            throw new GradleException("Quarkus native build for '" + request.descriptor().name()
                    + "' did not report a native executable path");
        }
        return new NativeResult(
                request.descriptor().name(),
                request.descriptor().type(),
                request.outputRoot(),
                outputName(request),
                executable,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                result.getGraalVMInfo() == null ? Map.of() : result.getGraalVMInfo(),
                artifacts(result.getResults()));
    }

    private static NativeResult nativeSources(BuildRequest request, AugmentResult result) {
        Path sourcesDirectory = request.outputRoot().resolve("native-sources");
        return new NativeResult(
                request.descriptor().name(),
                request.descriptor().type(),
                request.outputRoot(),
                outputName(request),
                Optional.empty(),
                Optional.of(sourcesDirectory),
                artifactPath(result, NATIVE_SOURCES),
                Optional.of(sourcesDirectory.resolve("native-image.args")),
                result.getGraalVMInfo() == null ? Map.of() : result.getGraalVMInfo(),
                artifacts(result.getResults()));
    }

    private static String outputName(BuildRequest request) {
        return request.buildSystemProperties().getOrDefault("quarkus.package.output-name",
                request.descriptor().name());
    }

    private static Optional<Path> artifactPath(AugmentResult result, String type) {
        if (result.getResults() == null) {
            return Optional.empty();
        }
        return result.getResults().stream()
                .filter(artifact -> type.equals(artifact.getType()))
                .map(ArtifactResult::getPath)
                .filter(path -> path != null)
                .findFirst();
    }

    private static List<NativeResult.Artifact> artifacts(List<ArtifactResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(result -> result.getType() != null && !result.getType().isBlank())
                .map(result -> new NativeResult.Artifact(
                        Optional.ofNullable(result.getPath()),
                        result.getType(),
                        result.getMetadata() == null ? Map.of() : result.getMetadata()))
                .toList();
    }
}
