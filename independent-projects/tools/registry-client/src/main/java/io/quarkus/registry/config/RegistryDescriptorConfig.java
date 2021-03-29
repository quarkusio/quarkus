package io.quarkus.registry.config;

import io.quarkus.maven.ArtifactCoords;

/**
 * Configuration related to resolution of the registry descriptor.
 * Registry descriptor represents the default client configuration to communicate with the registry
 * that can be customized, if necessary, on the client side.
 */
public interface RegistryDescriptorConfig {

    /**
     * Coordinates of the registry descriptor artifact in the registry.
     *
     * @return coordinates of the registry descriptor artifact in the registry
     */
    ArtifactCoords getArtifact();
}
