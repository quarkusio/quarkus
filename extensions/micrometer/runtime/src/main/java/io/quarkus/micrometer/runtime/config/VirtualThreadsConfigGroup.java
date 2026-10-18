package io.quarkus.micrometer.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for the virtual thread metric collection.
 */
@ConfigGroup
public interface VirtualThreadsConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Virtual Threads metrics support.
     * <p>
     * Support for virtual threads metrics will be enabled if Micrometer support is enabled,
     * this value is set to {@code true} (default), the JVM supports virtual threads (Java 21+) and the
     * {@code quarkus.micrometer.binder-enabled-default} property is true.
     */
    @Override
    Optional<Boolean> enabled();

    /**
     * The tags to be added to the metrics.
     * Empty by default.
     * When set, tags are passed as: {@code key1=value1,key2=value2}.
     */
    Optional<List<String>> tags();
}
