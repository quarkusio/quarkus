package io.quarkus.micrometer.runtime.config;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface JsonConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * Support for export to JSON format. Off by default.
     */
    @WithDefault("false")
    @Override
    Optional<Boolean> enabled();

    /**
     * The path for the JSON metrics endpoint.
     * The default value is {@code metrics}.
     *
     * By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     * If the management interface is enabled, the value will be resolved as a path relative to
     * `${quarkus.management.root-path}`.
     */
    @WithDefault("metrics")
    String path();

    /**
     * Statistics like max, percentiles, and histogram counts decay over time to give greater weight to recent
     * samples. Samples are accumulated to such statistics in ring buffers which rotate after
     * the expiry, with this buffer length.
     */
    @WithDefault("3")
    Integer bufferLength();

    /**
     * Statistics like max, percentiles, and histogram counts decay over time to give greater weight to recent
     * samples. Samples are accumulated to such statistics in ring buffers which rotate after
     * this expiry, with a particular buffer length.
     */
    @WithDefault("P3D")
    Duration expiry();
}
