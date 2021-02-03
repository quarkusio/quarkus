package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Build / static runtime config for outbound HTTP requests
 */
@ConfigGroup
public class HttpClientConfigGroup implements MicrometerConfig.CapabilityEnabled {
    /**
     * Outbound HTTP request metrics support.
     * <p>
     * Support for HTTP client metrics will be enabled if Micrometer
     * support is enabled, the REST client feature is enabled,
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @ConfigItem
    public Optional<Boolean> enabled;

    @Override
    public Optional<Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{enabled=" + enabled
                + '}';
    }
}
