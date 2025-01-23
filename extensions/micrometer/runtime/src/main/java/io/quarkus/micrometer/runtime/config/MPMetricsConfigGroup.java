package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for the Microprofile Metrics Binder
 */
@ConfigGroup
public interface MPMetricsConfigGroup extends MicrometerConfig.CapabilityEnabled {
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
    @Override
    Optional<Boolean> enabled();
}
