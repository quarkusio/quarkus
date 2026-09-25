package io.quarkus.gradle.application.internal.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

public record DeploymentResult(
        String buildName,
        String deploymentName,
        QuarkusApplicationDeploymentTarget target,
        QuarkusApplicationDeploymentImageSource imageSource,
        String imageReference,
        Optional<String> quarkusDeployTarget,
        Optional<String> kubernetesDeploymentTarget,
        Optional<String> resultName,
        Map<String, String> resultLabels,
        boolean success) {

    public static final String SCHEMA_VERSION = "1";

    public DeploymentResult {
        if (buildName == null || buildName.isBlank()) {
            throw new IllegalArgumentException("Deployment result build name must not be empty");
        }
        if (deploymentName == null || deploymentName.isBlank()) {
            throw new IllegalArgumentException("Deployment result deployment name must not be empty");
        }
        requireNonNull(target, "target");
        requireNonNull(imageSource, "imageSource");
        if (imageReference == null || imageReference.isBlank()) {
            throw new IllegalArgumentException("Deployment result image reference must not be empty");
        }
        requireNonNull(quarkusDeployTarget, "quarkusDeployTarget");
        requireNonNull(kubernetesDeploymentTarget, "kubernetesDeploymentTarget");
        requireNonNull(resultName, "resultName");
        resultLabels = Map.copyOf(requireNonNull(resultLabels, "resultLabels"));
    }
}
