package io.quarkus.micrometer.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ConfigWithType;

/**
 * LoadBalancerLoader for io.quarkus.it.rest.client.reactive.stork.MockLoadBalancerProvider
 */
@ApplicationScoped
public class MockServiceSelectorProviderLoader implements io.smallrye.stork.spi.internal.LoadBalancerLoader {
    private final MockServiceSelectorProvider provider;

    public MockServiceSelectorProviderLoader() {
        MockServiceSelectorProvider actual = null;
        try {
            actual = CDI.current().select(MockServiceSelectorProvider.class).get();
        } catch (Exception e) {
            // Use direct instantiation
            actual = new MockServiceSelectorProvider();
        }
        this.provider = actual;
    }

    @Override
    public LoadBalancer createLoadBalancer(ConfigWithType config, ServiceDiscovery serviceDiscovery) {
        io.quarkus.micrometer.test.MockServiceSelectorConfiguration typedConfig = new io.quarkus.micrometer.test.MockServiceSelectorConfiguration(
                config.parameters());
        return provider.createLoadBalancer(typedConfig, serviceDiscovery);
    }

    /**
     * @return the type
     */
    @Override
    public String type() {
        return "mock";
    }
}
