package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StorkLoadBalancerConfiguration {

    /**
     * Configures load balancer type, e.g. "round-robin".
     * A LoadBalancerProvider for the type has to be available
     *
     */
    @ConfigItem(defaultValue = "round-robin")
    public String type;

    /**
     * Load Balancer parameters.
     * Check the documentation of the selected load balancer type for available parameters
     *
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, String> parameters;

}
