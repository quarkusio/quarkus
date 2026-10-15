package io.quarkus.deployment.builditem;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Indicates that the OpenTelemetry SDK is present and carries the build-time enabled state
 * for each telemetry signal (tracing, metrics, logging) and a check if the OTel SDK is enabled at runtime.
 * This can be used to decide if OTel instrumentation needs to be instantiated, or activated.
 * <p>
 * Produced by the OpenTelemetry extension's deployment module. Other extensions should consume
 * it as an {@link Optional} so they still work when the OpenTelemetry extension is not on the
 * classpath.
 * <p>
 * The build-time flags reflect the following configuration properties:
 * <ul>
 * <li>{@code quarkus.otel.traces.enabled} — tracing (defaults to {@code true} when the SDK is enabled)</li>
 * <li>{@code quarkus.otel.metrics.enabled} — metrics (defaults to {@code false})</li>
 * <li>{@code quarkus.otel.logs.enabled} — logging (defaults to {@code false})</li>
 * </ul>
 * Each signal is only active when both its own flag <em>and</em> the top-level
 * {@code quarkus.otel.enabled} flag are {@code true}.
 *
 * <h2>Usage example</h2>
 *
 * <pre>{@code
 * &#64;BuildStep
 * void configureTracing(Optional<OpenTelemetrySdkBuildItem> otelSdk, ...) {
 *     if (otelSdk.isPresent() && otelSdk.get().isTracingBuildTimeEnabled()) {
 *         // register tracing integration
 *     }
 * }
 * }</pre>
 *
 * The convenience method {@link #isOtelSdkEnabled(Optional)} extracts the runtime-enabled
 * {@link RuntimeValue} when the build item is present:
 *
 * <pre>{@code
 * Optional<RuntimeValue<Boolean>> runtimeEnabled = OpenTelemetrySdkBuildItem.isOtelSdkEnabled(otelSdk);
 * }</pre>
 */
public final class OpenTelemetrySdkBuildItem extends SimpleBuildItem {

    private final boolean tracingBuildTimeEnabled;
    private final boolean metricsBuildTimeEnabled;
    private final boolean loggingBuildTimeEnabled;

    private final RuntimeValue<Boolean> runtimeEnabled;

    public OpenTelemetrySdkBuildItem(boolean tracingBuildTimeEnabled, boolean metricsBuildTimeEnabled,
            boolean loggingBuildTimeEnabled, RuntimeValue<Boolean> runtimeEnabled) {
        this.tracingBuildTimeEnabled = tracingBuildTimeEnabled;
        this.metricsBuildTimeEnabled = metricsBuildTimeEnabled;
        this.loggingBuildTimeEnabled = loggingBuildTimeEnabled;
        this.runtimeEnabled = runtimeEnabled;
    }

    /**
     * @return {@code true} if OpenTelemetry Tracing is enabled at build time
     */
    public boolean isTracingBuildTimeEnabled() {
        return tracingBuildTimeEnabled;
    }

    /**
     * @return {@code true} if OpenTelemetry Metrics is enabled at build time
     */
    public boolean isMetricsBuildTimeEnabled() {
        return metricsBuildTimeEnabled;
    }

    /**
     * @return {@code true} if OpenTelemetry Logging is enabled at build time
     */
    public boolean isLoggingBuildTimeEnabled() {
        return loggingBuildTimeEnabled;
    }

    /**
     * True if the OpenTelemetry SDK is enabled at build and runtime.
     */
    public RuntimeValue<Boolean> isRuntimeEnabled() {
        return runtimeEnabled;
    }

    public static Optional<RuntimeValue<Boolean>> isOtelSdkEnabled(Optional<OpenTelemetrySdkBuildItem> buildItem) {
        // optional is empty if the extension is disabled at build time
        return buildItem.isPresent() ? Optional.of(buildItem.get().isRuntimeEnabled()) : Optional.empty();
    }
}
