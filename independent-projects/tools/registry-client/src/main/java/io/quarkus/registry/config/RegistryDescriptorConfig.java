package io.quarkus.registry.config;

import io.quarkus.maven.ArtifactCoords;

public interface RegistryDescriptorConfig {

    ArtifactCoords getArtifact();
}
