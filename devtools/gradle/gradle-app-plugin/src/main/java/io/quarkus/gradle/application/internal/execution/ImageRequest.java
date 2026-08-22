package io.quarkus.gradle.application.internal.execution;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.quarkus.gradle.application.internal.image.ContainerImageTarget;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

public record ImageRequest(
        BuildRequest build,
        ImageOperation operation,
        Optional<ContainerImageTarget> target,
        Optional<QuarkusApplicationImageBuilder> builder,
        Map<String, String> commonBuildProperties,
        Map<String, String> imageBuildProperties,
        Path receiptFile,
        Optional<Path> jibDigestFile,
        Optional<Path> jibImageIdFile) {

    public ImageRequest {
        if (build == null) {
            throw new IllegalArgumentException("Quarkus application image request requires a build request");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Quarkus application image request requires an operation");
        }
        target = target == null ? Optional.empty() : target;
        builder = builder == null ? Optional.empty() : builder;
        commonBuildProperties = Map.copyOf(commonBuildProperties);
        imageBuildProperties = Map.copyOf(imageBuildProperties);
        if (receiptFile == null) {
            throw new IllegalArgumentException("Quarkus application image request requires a receipt file");
        }
        if (jibDigestFile == null || jibImageIdFile == null) {
            throw new IllegalArgumentException("Quarkus application image request requires Jib metadata optionals");
        }
    }
}
