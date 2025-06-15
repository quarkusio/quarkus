package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface StorkServiceDiscoveryConfiguration {

    /**
     * Configures the service discovery type, e.g. "consul". ServiceDiscoveryProvider for the type has to be available
     */
    String type();

    /**
     * ServiceDiscovery parameters. Check the documentation of the selected service discovery type for available
     * parameters.
     */
    @WithParentName
    Map<String, String> params();

}
