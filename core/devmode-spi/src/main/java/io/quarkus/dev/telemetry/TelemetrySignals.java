package io.quarkus.dev.telemetry;

/**
 * The signals the dev mode tooling gives a meaning to. A {@link TelemetryEvent} may carry any signal; one that no
 * receiver knows is simply not shown.
 */
public final class TelemetrySignals {

    /**
     * One reading of a meter, charted on the Dev UI observability dashboard. Build it with
     * {@link TelemetryEvent#metric(String)}.
     */
    public static final String METRIC = "metric";

    /**
     * One finished span of a trace, shown in the Dev UI traces view. Build it with {@link TelemetryEvent#span(String)}.
     */
    public static final String SPAN = "span";

    private TelemetrySignals() {
    }
}
