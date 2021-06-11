package io.quarkus.micrometer.runtime.config;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JsonConfigGroup implements MicrometerConfig.CapabilityEnabled {
    /**
     * Support for export to JSON format. Off by default.
     */
    @ConfigItem(defaultValue = "false")
    public Optional<Boolean> enabled;

    /**
     * The path for the JSON metrics endpoint.
     * The default value is {@code metrics}.
     */
    @ConfigItem(defaultValue = "metrics")
    public String path;

    /**
     * Statistics like max, percentiles, and histogram counts decay over time to give greater weight to recent
     * samples. Samples are accumulated to such statistics in ring buffers which rotate after
     * the expiry, with this buffer length.
     */
    @ConfigItem(defaultValue = "3")
    public Integer bufferLength;

    /**
     * Statistics like max, percentiles, and histogram counts decay over time to give greater weight to recent
     * samples. Samples are accumulated to such statistics in ring buffers which rotate after
     * this expiry, with a particular buffer length.
     */
    @ConfigItem(defaultValue = "P3D")
    public Duration expiry;

    @Override
    public Optional<Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{path='" + path
                + ",enabled=" + enabled
                + '}';
    }
}
