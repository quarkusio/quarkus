package io.quarkus.devtools.testing.registry.client;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.config.RegistryConfig;

public class TestRegistryClientFactory implements RegistryClientFactory {

    private static TestRegistryClientFactory instance;

    public static TestRegistryClientFactory getInstance(RegistryClientEnvironment env) {
        if (instance != null) {
            return instance;
        }
        if (Thread.currentThread().getContextClassLoader() instanceof QuarkusClassLoader) {
            ((QuarkusClassLoader) Thread.currentThread().getContextClassLoader()).addCloseTask(() -> instance = null);
        }
        return instance = new TestRegistryClientFactory(env);
    }

    private final RegistryClientEnvironment env;

    private TestRegistryClientFactory(RegistryClientEnvironment env) {
        this.env = env;
    }

    @Override
    public RegistryClient buildRegistryClient(RegistryConfig config) throws RegistryResolutionException {
        return new TestRegistryClient(env, config);
    }

    public static void reset() {
        instance = null;
    }
}
