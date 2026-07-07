package io.quarkus.gradle.application.internal.execution;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

public record DeploymentRequest(
        BuildRequest build,
        String deploymentName,
        QuarkusApplicationDeploymentTarget target,
        QuarkusApplicationDeploymentImageSource imageSource,
        String imageReference,
        Path receiptFile) {

    public DeploymentRequest {
        requireNonNull(build, "build");
        if (deploymentName == null || deploymentName.isBlank()) {
            throw new IllegalArgumentException("Deployment name must not be empty");
        }
        requireNonNull(target, "target");
        requireNonNull(imageSource, "imageSource");
        if (imageReference == null || imageReference.isBlank()) {
            throw new IllegalArgumentException("Deployment image reference must not be empty");
        }
        requireNonNull(receiptFile, "receiptFile");
    }
}
