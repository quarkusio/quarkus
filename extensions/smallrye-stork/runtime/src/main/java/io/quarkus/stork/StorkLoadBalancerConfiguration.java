package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface StorkLoadBalancerConfiguration {

    /**
     * Configures load balancer type, e.g. "round-robin".
     * A LoadBalancerProvider for the type has to be available
     *
     */
    @WithDefault(value = "round-robin")
    String type();

    /**
     * Load Balancer parameters.
     * Check the documentation of the selected load balancer type for available parameters
     *
     */
    @WithParentName
    Map<String, String> parameters();

}
