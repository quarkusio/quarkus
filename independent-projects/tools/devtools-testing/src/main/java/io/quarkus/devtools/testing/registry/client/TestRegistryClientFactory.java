package io.quarkus.devtools.testing.registry.client;

import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.config.RegistryConfig;

public class TestRegistryClientFactory implements RegistryClientFactory {

    private static TestRegistryClientFactory instance;

    public static TestRegistryClientFactory getInstance(RegistryClientEnvironment env) {
        return instance == null ? instance = new TestRegistryClientFactory(env) : instance;
    }

    private final RegistryClientEnvironment env;

    private TestRegistryClientFactory(RegistryClientEnvironment env) {
        this.env = env;
    }

    @Override
    public RegistryClient buildRegistryClient(RegistryConfig config) throws RegistryResolutionException {
        return new TestRegistryClient(env, config);
    }
}
