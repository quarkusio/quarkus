package io.quarkus.gradle.application.internal.execution;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

public record StartupOptimizedImageRequest(
        BuildRequest build,
        ImageOperation operation,
        BuiltContainerImage baseImage,
        Path baseImageReceiptFile,
        QuarkusApplicationJvmStartupArchiveType archiveType,
        Path archive,
        String optimizedImageReference,
        Optional<QuarkusApplicationImageBuilder> builder,
        Path receiptFile) {

    public StartupOptimizedImageRequest {
        if (build == null) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires a build request");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires an operation");
        }
        if (baseImage == null) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires a base image");
        }
        if (baseImage.reference().isEmpty()) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires a base image reference");
        }
        if (baseImage.workingDirectory().isEmpty()) {
            throw new IllegalArgumentException(
                    "Quarkus startup-optimized image request requires a base image working directory");
        }
        if (baseImageReceiptFile == null) {
            throw new IllegalArgumentException(
                    "Quarkus startup-optimized image request requires a base image receipt file");
        }
        if (archiveType == null) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires an archive type");
        }
        if (archive == null) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires an archive");
        }
        if (optimizedImageReference == null || optimizedImageReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Quarkus startup-optimized image request requires an optimized image reference");
        }
        if (optimizedImageReference.equals(baseImage.reference().orElseThrow())) {
            throw new IllegalArgumentException(
                    "Quarkus startup-optimized image reference must differ from its archive-free base image");
        }
        builder = builder == null ? Optional.empty() : builder;
        if (receiptFile == null) {
            throw new IllegalArgumentException("Quarkus startup-optimized image request requires a receipt file");
        }
    }
}
