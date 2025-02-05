package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for the Vert.x Binder
 */
@ConfigGroup
public interface VertxConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Vert.x metrics support.
     * <p>
     * Support for Vert.x metrics will be enabled if Micrometer
     * support is enabled, Vert.x MetricsOptions is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     *
     */
    @Override
    Optional<Boolean> enabled();
}
