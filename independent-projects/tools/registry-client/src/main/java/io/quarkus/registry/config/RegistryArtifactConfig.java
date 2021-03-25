package io.quarkus.registry.config;

import io.quarkus.maven.ArtifactCoords;

/**
 * Base registry catalog artifact configuration
 */
public interface RegistryArtifactConfig {

    /**
     * Whether this catalog artifact is not supported by the registry or should simply be disabled on the client side.
     *
     * @return true, if the catalog represented by this artifact should be excluded from processing
     */
    boolean isDisabled();

    /**
     * Catalog artifact coordinates in the registry.
     *
     * @return catalog artifact coordinates in the registry
     */
    ArtifactCoords getArtifact();
}
