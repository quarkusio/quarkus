package io.quarkus.registry.config;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

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

    /** @return a mutable copy of this config */
    default Mutable mutable() {
        return new RegistryArtifactConfigImpl.Builder(this);
    }

    interface Mutable extends RegistryArtifactConfig, JsonBuilder<RegistryArtifactConfig> {

        Mutable setArtifact(ArtifactCoords artifact);

        Mutable setDisabled(boolean disabled);

        /** @return an immutable copy of this config */
        RegistryArtifactConfig build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new RegistryArtifactConfigImpl.Builder();
    }
}
