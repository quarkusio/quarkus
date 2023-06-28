package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StorkServiceRegistrarConfiguration {

    /**
     * Configures service registrar type, e.g. "consul".
     * A ServiceRegistrarProvider for the type has to be available
     *
     */
    @ConfigItem
    public String type;

    /**
     * Service Registrar parameters.
     * Check the documentation of the selected registrar type for available parameters
     *
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, String> parameters;

}
