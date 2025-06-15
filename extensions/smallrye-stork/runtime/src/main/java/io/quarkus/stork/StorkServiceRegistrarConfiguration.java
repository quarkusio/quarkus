package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface StorkServiceRegistrarConfiguration {

    /**
     * Configures service registrar type, e.g. "consul". A ServiceRegistrarProvider for the type has to be available
     */
    String type();

    /**
     * Service Registrar parameters. Check the documentation of the selected registrar type for available parameters
     */
    // @ConfigItem(name = ConfigItem.PARENT)
    @WithParentName
    Map<String, String> parameters();

}
