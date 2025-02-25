package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for Redis metrics
 */
@ConfigGroup
public interface RedisConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Redis client metrics support.
     * <p>
     * Support for Redis metrics will be enabled if Micrometer support is enabled,
     * the Quarkus Redis client extension is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
