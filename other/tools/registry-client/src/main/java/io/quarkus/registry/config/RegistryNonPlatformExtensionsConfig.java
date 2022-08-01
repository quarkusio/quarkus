package io.quarkus.registry.config;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Configuration related to the resolution of catalogs of non-platform extensions.
 */
public interface RegistryNonPlatformExtensionsConfig extends RegistryArtifactConfig {

    @Override
    default RegistryNonPlatformExtensionsConfig.Mutable mutable() {
        return new RegistryNonPlatformExtensionsConfigImpl.Builder(this);
    }

    interface Mutable extends RegistryNonPlatformExtensionsConfig, RegistryArtifactConfig.Mutable {

        @Override
        RegistryNonPlatformExtensionsConfig.Mutable setArtifact(ArtifactCoords artifact);

        @Override
        RegistryNonPlatformExtensionsConfig.Mutable setDisabled(boolean disabled);

        /** @return an immutable copy of this config */
        @Override
        RegistryNonPlatformExtensionsConfig build();

        @Override
        default RegistryNonPlatformExtensionsConfig.Mutable mutable() {
            return new RegistryNonPlatformExtensionsConfigImpl.Builder(this);
        }
    }

    /**
     * @return a new mutable instance
     */
    static RegistryNonPlatformExtensionsConfig.Mutable builder() {
        return new RegistryNonPlatformExtensionsConfigImpl.Builder();
    }
}
