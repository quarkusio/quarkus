package io.quarkus.devui.runtime.observability.metrics.config;

import java.time.Duration;

import io.quarkus.dev.telemetry.TelemetryEvents;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings for the dev-mode metrics capture and per-series buffers.
 */
@ConfigMapping(prefix = "quarkus.dev-ui.observability.metrics")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MetricsDevUiRuntimeConfig {

    /**
     * How often meters are sampled.
     * <p>
     * The metrics backends read it without depending on Dev UI, through
     * {@link TelemetryEvents#metricsSampleInterval}, which is where the default comes from.
     */
    @WithDefault(TelemetryEvents.DEFAULT_METRICS_SAMPLE_INTERVAL)
    Duration sampleInterval();

    /** Rolling time window kept per series (120 points at the 5s default). */
    @WithDefault("10m")
    Duration retention();

    /** Hard cap on points per series (1h at the 5s default). Safety bound. */
    @WithDefault("720")
    int maxPointsPerSeries();
}
