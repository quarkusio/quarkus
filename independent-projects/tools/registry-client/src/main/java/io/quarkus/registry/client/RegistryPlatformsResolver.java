package io.quarkus.registry.client;

import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.PlatformCatalog;

public interface RegistryPlatformsResolver {

    /**
     * Returns a catalog of the recommended platform versions, indicating which one of them
     * is the default one for new project creation, for a given Quarkus version or in general,
     * in case the caller did not provide any specific Quarkus version.
     *
     * @param quarkusVersion Quarkus version or null
     * @return catalog of the recommended platform versions
     * @throws RegistryResolutionException in case of a failure
     */
    PlatformCatalog resolvePlatforms(String quarkusVersion) throws RegistryResolutionException;
}
