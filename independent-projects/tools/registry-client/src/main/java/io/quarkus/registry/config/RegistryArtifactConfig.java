package io.quarkus.registry.config;

import io.quarkus.maven.ArtifactCoords;

public interface RegistryArtifactConfig {

    boolean isDisabled();

    ArtifactCoords getArtifact();
}
