package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Build / static runtime config for the Microprofile Metrics Binder
 */
@ConfigGroup
public class MPMetricsConfigGroup implements MicrometerConfig.CapabilityEnabled {
    // @formatter:off
    /**
     * Eclipse MicroProfile Metrics support.
     *
     * Support for MicroProfile Metrics will be enabled if Micrometer
     * support is enabled and the MicroProfile Metrics dependency is present:
     *
     * [source,xml]
     * ----
     * <dependency>
     *   <groupId>org.eclipse.microprofile.metrics</groupId>
     *   <artifactId>microprofile-metrics-api</artifactId>
     * </dependency>
     * ----
     *
     * The Micrometer extension currently provides a compatibility layer that supports the MP Metrics API,
     * but metric names and recorded values will be different.
     * Note that the MP Metrics compatibility layer will move to a different extension in the future.
     *
     * @asciidoclet
     */
    // @formatter:on
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
