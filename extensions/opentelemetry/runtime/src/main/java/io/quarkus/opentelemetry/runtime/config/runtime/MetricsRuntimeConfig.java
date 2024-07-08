package io.quarkus.opentelemetry.runtime.config.runtime;

import java.time.Duration;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface MetricsRuntimeConfig {

    /**
     * The interval, between the start of two metric export attempts.
     * <p>
     * Default is 1min.
     *
     * @return the interval Duration.
     */
    @WithName("export.interval")
    @WithDefault("60s")
    Duration exportInterval();
}
