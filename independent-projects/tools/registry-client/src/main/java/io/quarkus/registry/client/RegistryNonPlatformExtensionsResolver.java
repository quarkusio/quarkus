package io.quarkus.registry.client;

import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

public interface RegistryNonPlatformExtensionsResolver {

    /**
     * Returns a catalog of extensions that are compatible with a given Quarkus version
     * or null, in case the registry does not include any extension that is compatible
     * with the given Quarkus version.
     *
     * @param quarkusVersion Quarkus version
     * @return catalog of extensions compatible with a given Quarkus version or null
     * @throws RegistryResolutionException in case of a failure
     */
    ExtensionCatalog resolveNonPlatformExtensions(String quarkusVersion)
            throws RegistryResolutionException;
}
