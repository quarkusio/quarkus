package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for inbound HTTP traffic
 */
@ConfigGroup
public interface HttpServerConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Inbound HTTP metrics support.
     * <p>
     * Support for HTTP server metrics will be enabled if Micrometer
     * support is enabled, an extension serving HTTP traffic is enabled,
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
