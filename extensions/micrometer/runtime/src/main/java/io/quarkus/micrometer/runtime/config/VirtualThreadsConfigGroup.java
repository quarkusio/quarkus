package io.quarkus.micrometer.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Build / static runtime config for the virtual thread metric collection.
 */
@ConfigGroup
public class VirtualThreadsConfigGroup implements MicrometerConfig.CapabilityEnabled {
    /**
     * Virtual Threads metrics support.
     * <p>
     * Support for virtual threads metrics will be enabled if Micrometer support is enabled,
     * this value is set to {@code true} (default), the JVM supports virtual threads (Java 21+) and the
     * {@code quarkus.micrometer.binder-enabled-default} property is true.
     */
    @ConfigItem
    public Optional<Boolean> enabled;
    /**
     * The tags to be added to the metrics.
     * Empty by default.
     * When set, tags are passed as: {@code key1=value1,key2=value2}.
     */
    @ConfigItem
    public Optional<List<String>> tags;

    @Override
    public Optional<Boolean> getEnabled() {
        return enabled;
    }
}
