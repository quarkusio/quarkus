package io.quarkus.registry.client.spi;

import io.quarkus.registry.client.RegistryClientFactory;

/**
 * Registry client factory service provider interface that will be looked up on the class path using the ServiceLoader
 * mechanism.
 */
public interface RegistryClientFactoryProvider {

    RegistryClientFactory newRegistryClientFactory(RegistryClientEnvironment env);
}
