package io.quarkus.gradle.application.internal.deployment;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;

public record DeploymentImageSourceRequest(
        QuarkusApplicationDeploymentImageSource imageSource,
        Optional<String> explicitImageReference,
        Optional<Path> normalImagePushReceipt,
        Optional<Path> startupOptimizedImagePushReceipt) {

    public DeploymentImageSourceRequest {
        requireNonNull(imageSource, "imageSource");
        requireNonNull(explicitImageReference, "explicitImageReference");
        requireNonNull(normalImagePushReceipt, "normalImagePushReceipt");
        requireNonNull(startupOptimizedImagePushReceipt, "startupOptimizedImagePushReceipt");
    }
}
