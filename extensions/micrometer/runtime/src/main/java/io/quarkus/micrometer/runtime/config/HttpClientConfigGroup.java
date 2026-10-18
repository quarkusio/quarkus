package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for outbound HTTP requests
 */
@ConfigGroup
public interface HttpClientConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Outbound HTTP request metrics support.
     * <p>
     * Support for HTTP client metrics will be enabled if Micrometer
     * support is enabled, the REST client feature is enabled,
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
