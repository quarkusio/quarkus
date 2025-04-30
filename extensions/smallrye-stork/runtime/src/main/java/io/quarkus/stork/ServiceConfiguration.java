package io.quarkus.stork;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface ServiceConfiguration {
    /**
     * ServiceDiscovery configuration for the service
     */
    Optional<StorkServiceDiscoveryConfiguration> serviceDiscovery();

    /**
     * LoadBalancer configuration for the service
     */
    StorkLoadBalancerConfiguration loadBalancer();

    /**
     * ServiceRegistrar configuration for the service
     */
    Optional<StorkServiceRegistrarConfiguration> serviceRegistrar();
}
