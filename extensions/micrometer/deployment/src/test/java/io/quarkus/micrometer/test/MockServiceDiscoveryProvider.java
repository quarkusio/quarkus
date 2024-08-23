package io.quarkus.micrometer.test;

import jakarta.enterprise.context.ApplicationScoped;

import org.mockito.Mockito;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("mock")
@ApplicationScoped
public class MockServiceDiscoveryProvider implements ServiceDiscoveryProvider<MockServiceDiscoveryConfiguration> {

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    private final ServiceDiscovery serviceDiscovery = Mockito.mock(ServiceDiscovery.class);;

    @Override
    public ServiceDiscovery createServiceDiscovery(MockServiceDiscoveryConfiguration config, String serviceName,
            ServiceConfig serviceConfig,
            StorkInfrastructure storkInfrastructure) {
        return serviceDiscovery;
    }

}
