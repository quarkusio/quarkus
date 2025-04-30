package io.quarkus.micrometer.test;

import jakarta.enterprise.context.ApplicationScoped;

import org.mockito.Mockito;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("mock")
@ApplicationScoped
public class MockServiceSelectorProvider implements LoadBalancerProvider<MockServiceSelectorConfiguration> {

    private final LoadBalancer loadBalancer = Mockito.mock(LoadBalancer.class);

    @Override
    public LoadBalancer createLoadBalancer(MockServiceSelectorConfiguration config, ServiceDiscovery serviceDiscovery) {
        return loadBalancer;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }
}
