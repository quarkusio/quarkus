package io.quarkus.gradle.application.internal.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public record DeploymentImageSourceResolution(String imageReference,
        Map<String, String> forcedProperties) {

    public DeploymentImageSourceResolution {
        if (imageReference == null || imageReference.isBlank()) {
            throw new IllegalArgumentException("Deployment image reference must not be empty");
        }
        forcedProperties = Map.copyOf(requireNonNull(forcedProperties, "forcedProperties"));
    }
}
