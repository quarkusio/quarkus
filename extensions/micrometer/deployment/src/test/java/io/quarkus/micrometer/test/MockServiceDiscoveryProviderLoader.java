package io.quarkus.micrometer.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.StorkInfrastructure;

/**
 * ServiceDiscoveryLoader for {@link io.quarkus.micrometer.test.MockServiceDiscoveryProvider}
 */
@ApplicationScoped
public class MockServiceDiscoveryProviderLoader implements io.smallrye.stork.spi.internal.ServiceDiscoveryLoader {
    private final io.quarkus.micrometer.test.MockServiceDiscoveryProvider provider;

    public MockServiceDiscoveryProviderLoader() {
        io.quarkus.micrometer.test.MockServiceDiscoveryProvider actual = null;
        try {
            actual = CDI.current().select(io.quarkus.micrometer.test.MockServiceDiscoveryProvider.class).get();
        } catch (Exception e) {
            // Use direct instantiation
            actual = new io.quarkus.micrometer.test.MockServiceDiscoveryProvider();
        }
        this.provider = actual;
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(ConfigWithType config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        MockServiceDiscoveryConfiguration typedConfig = new MockServiceDiscoveryConfiguration(
                config.parameters());
        return provider.createServiceDiscovery(typedConfig, serviceName, serviceConfig, storkInfrastructure);
    }

    /**
     * @return the type
     */
    @Override
    public String type() {
        return "mock";
    }
}
