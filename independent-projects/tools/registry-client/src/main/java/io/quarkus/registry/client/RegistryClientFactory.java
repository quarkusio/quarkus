package io.quarkus.registry.client;

import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.config.RegistryConfig;

public interface RegistryClientFactory {

    RegistryClient buildRegistryClient(RegistryConfig config) throws RegistryResolutionException;
}
