package io.quarkus.stork;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface StorkServiceRegistrarConfiguration {

    /**
     * Whether automatic registration and deregistration of service instances is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Configures service registrar type, e.g. "consul".
     * A ServiceRegistrarProvider for the type has to be available
     *
     */
    Optional<String> type();

    /**
     * Service Registrar parameters.
     * Check the documentation of the selected registrar type for available parameters
     *
     */
    @WithParentName
    Map<String, String> parameters();

}
