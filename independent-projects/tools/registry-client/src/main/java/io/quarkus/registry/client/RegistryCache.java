package io.quarkus.registry.client;

import io.quarkus.registry.RegistryResolutionException;

public interface RegistryCache {

    void clearCache() throws RegistryResolutionException;
}
