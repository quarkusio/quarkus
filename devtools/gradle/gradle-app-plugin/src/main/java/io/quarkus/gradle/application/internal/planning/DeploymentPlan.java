package io.quarkus.gradle.application.internal.planning;

import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;

public record DeploymentPlan(QuarkusApplicationDeploymentDescriptor deployment, String taskName,
        QuarkusApplicationDeploymentImageSource imageSource, Optional<String> imageReference) {
}
