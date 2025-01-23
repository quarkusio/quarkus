package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for Reactive Messaging Binders
 */
@ConfigGroup
public interface ReactiveMessagingConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Kafka metrics support.
     * <p>
     * Support for Reactive Messaging metrics will be enabled if Micrometer support is enabled,
     * MessageObservationCollector interface is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();
}
