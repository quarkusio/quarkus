package io.quarkus.registry.client;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

public interface RegistryPlatformExtensionsResolver {

    /**
     * Returns a catalog of extensions that represents a given platform.
     *
     * @param platformCoords either a BOM or a JSON descriptor coordinates
     * @return catalog of extensions that represents the platform
     * @throws RegistryResolutionException in case of a failure
     */
    ExtensionCatalog resolvePlatformExtensions(ArtifactCoords platformCoords)
            throws RegistryResolutionException;
}
