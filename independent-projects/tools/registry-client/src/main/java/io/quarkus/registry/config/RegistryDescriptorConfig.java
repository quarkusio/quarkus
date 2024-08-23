package io.quarkus.registry.config;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

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

    /** Return a mutable copy of this configuration */
    default Mutable mutable() {
        return new RegistryDescriptorConfigImpl.Builder(this);
    }

    interface Mutable extends RegistryDescriptorConfig, JsonBuilder<RegistryDescriptorConfig> {

        Mutable setArtifact(ArtifactCoords artifact);

        /** Return an immutable copy of this configuration */
        RegistryDescriptorConfig build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new RegistryDescriptorConfigImpl.Builder();
    }
}
