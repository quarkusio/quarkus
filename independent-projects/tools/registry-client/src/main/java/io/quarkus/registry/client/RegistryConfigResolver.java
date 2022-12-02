package io.quarkus.registry.client;

import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.config.RegistryConfig;

public interface RegistryConfigResolver {

    /**
     * Returns the complete registry configuration. The idea is that the default configuration
     * to communicate with the registry will be provided by the registry admins for all users.
     * Users though will still have a chance to adjust certain config options on the client side.
     *
     * @return complete registry configuration
     * @throws RegistryResolutionException in case of a failure
     */
    RegistryConfig resolveRegistryConfig() throws RegistryResolutionException;
}
