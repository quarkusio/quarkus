package io.quarkus.arc.deployment;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

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
