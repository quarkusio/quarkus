package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface StorkConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Stork metrics support.
     * <p>
     * Support for Stork metrics will be enabled if Micrometer support is enabled,
     * the Quarkus Stork extension is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
