package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StorkServiceDiscoveryConfiguration {

    /**
     * Configures the service discovery type, e.g. "consul".
     * ServiceDiscoveryProvider for the type has to be available
     *
     */
    @ConfigItem
    public String type;

    /**
     * ServiceDiscovery parameters.
     * Check the documentation of the selected service discovery type for available parameters.
     *
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, String> params;

}
