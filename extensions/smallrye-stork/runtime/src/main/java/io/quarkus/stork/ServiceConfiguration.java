package io.quarkus.stork;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ServiceConfiguration {
    /**
     * ServiceDiscovery configuration for the service
     */
    @ConfigItem
    public StorkServiceDiscoveryConfiguration serviceDiscovery;

    /**
     * LoadBalancer configuration for the service
     */
    @ConfigItem
    public StorkLoadBalancerConfiguration loadBalancer;
}
