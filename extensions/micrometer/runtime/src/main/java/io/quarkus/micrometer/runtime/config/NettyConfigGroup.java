package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for Netty Binders
 */
@ConfigGroup
public interface NettyConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Netty metrics support.
     * <p>
     * Support for Netty metrics will be enabled if Micrometer support is enabled,
     * the Netty allocator classes are on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
