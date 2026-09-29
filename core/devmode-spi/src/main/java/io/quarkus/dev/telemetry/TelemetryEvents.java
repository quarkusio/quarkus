package io.quarkus.dev.telemetry;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Whether anyone is receiving {@link TelemetryEvent}s, and the settings senders share with them.
 * <p>
 * Only dev mode tooling receives these events, so outside dev mode a sender would build each one for nobody. Checking
 * {@link #isEnabled()} first makes that cost a single volatile read:
 *
 * <pre>
 * if (TelemetryEvents.isEnabled()) {
 *     telemetry.fire(TelemetryEvent.metric("cache.size").value(cache.size()).build());
 * }
 * </pre>
 *
 * Firing without the check is still correct - an event nobody observes is simply dropped - just not free. This class
 * is loaded parent first in dev mode, so the flag is shared across live reloads.
 */
public final class TelemetryEvents {

    /**
     * How often metric senders should sample, as the Dev UI observability dashboard is configured. Read it with
     * {@link #metricsSampleInterval(BiFunction)}.
     */
    public static final String METRICS_SAMPLE_INTERVAL = "quarkus.dev-ui.observability.metrics.sample-interval";

    /**
     * The default of {@link #METRICS_SAMPLE_INTERVAL}, in the config syntax. The Dev UI config that declares the
     * property takes its default from here, so the two cannot drift apart.
     */
    public static final String DEFAULT_METRICS_SAMPLE_INTERVAL = "5s";

    // The config syntax for whole seconds (or minutes, hours) is the ISO-8601 one without its prefix.
    private static final Duration DEFAULT_METRICS_SAMPLE_INTERVAL_DURATION = Duration
            .parse("PT" + DEFAULT_METRICS_SAMPLE_INTERVAL);

    private static volatile boolean enabled;

    private TelemetryEvents() {
    }

    /**
     * @return whether dev mode tooling is receiving telemetry events
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * How often metric senders should sample: {@link #METRICS_SAMPLE_INTERVAL}, or its default when it is not set.
     * This class does not depend on the config API, so the sender passes its lookup:
     *
     * <pre>
     * Duration interval = TelemetryEvents.metricsSampleInterval(ConfigProvider.getConfig()::getOptionalValue);
     * </pre>
     *
     * @param config looks up a property as a {@link Duration}
     */
    public static Duration metricsSampleInterval(BiFunction<String, Class<Duration>, Optional<Duration>> config) {
        return config.apply(METRICS_SAMPLE_INTERVAL, Duration.class).orElse(DEFAULT_METRICS_SAMPLE_INTERVAL_DURATION);
    }

    /**
     * Called by the receiver when it starts receiving events, and with {@code false} when the application stops. Not
     * for senders.
     */
    public static void setEnabled(boolean enabled) {
        TelemetryEvents.enabled = enabled;
    }
}
