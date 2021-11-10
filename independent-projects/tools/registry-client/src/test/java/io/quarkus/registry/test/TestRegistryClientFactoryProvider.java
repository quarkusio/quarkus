package io.quarkus.registry.test;

import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.client.spi.RegistryClientFactoryProvider;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigImpl;
import io.quarkus.registry.config.RegistryConfigImpl;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Create a test client registry for tests within this project
 * to test in isolation from other things.
 */
public class TestRegistryClientFactoryProvider implements RegistryClientFactoryProvider {
    public final static Path testTargetPath = Paths.get("").normalize().toAbsolutePath().resolve("target");

    // Create a configuration that points back to this
    public final static RegistriesConfig createFakeRegistriesConfig() {
        return RegistriesConfigImpl.builder()
                .withRegistry(RegistryConfigImpl.builder()
                        .withId("test.quarkus.registry")
                        .withExtra("client-factory-url",
                                TestRegistryClientFactoryProvider.class.getProtectionDomain().getCodeSource().getLocation()
                                        .toExternalForm())
                        .withExtra("enable-maven-resolver", true)
                        .build())
                .build();
    }

    @Override
    public RegistryClientFactory newRegistryClientFactory(RegistryClientEnvironment env) {
        return null;
    }
}
