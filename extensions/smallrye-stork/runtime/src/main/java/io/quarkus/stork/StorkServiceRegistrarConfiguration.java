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
     * Optional name for this service instance as registered in the service registry.
     * When set, this value is used as the instance identifier instead of the auto-generated
     * {@code serviceName::ip::port} default. Must be unique within the service in the registry.
     */
    Optional<String> instanceName();

    /**
     * Service Registrar parameters.
     * Check the documentation of the selected registrar type for available parameters
     *
     */
    @WithParentName
    Map<String, String> parameters();

}
