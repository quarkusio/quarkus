package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for Kafka Binders
 */
@ConfigGroup
public interface KafkaConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Kafka metrics support.
     * <p>
     * Support for Kafka metrics will be enabled if Micrometer support is enabled,
     * the Kafka Consumer or Producer interface is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
