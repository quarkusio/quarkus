package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Build / static runtime config for inbound HTTP traffic
 */
@ConfigGroup
public class HttpServerConfigGroup implements MicrometerConfig.CapabilityEnabled {
    /**
     * Inbound HTTP metrics support.
     * <p>
     * Support for HTTP server metrics will be enabled if Micrometer
     * support is enabled, an extension serving HTTP traffic is enabled,
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
