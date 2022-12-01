package io.quarkus.devtools.testing.registry.client;

import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.client.spi.RegistryClientFactoryProvider;

public class TestRegistryClientFactoryProvider implements RegistryClientFactoryProvider {

    @Override
    public RegistryClientFactory newRegistryClientFactory(RegistryClientEnvironment env) {
        return TestRegistryClientFactory.getInstance(env);
    }
}
