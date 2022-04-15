package io.quarkus.opentelemetry.deployment.tracing;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.runtime.OpenTelemetryConfig;

public class TracerEnabled implements BooleanSupplier {
    OpenTelemetryConfig otelConfig;

    public boolean getAsBoolean() {
        return otelConfig.tracer.enabled.map(tracerEnabled -> otelConfig.enabled && tracerEnabled)
                .orElseGet(() -> otelConfig.enabled);
    }
}
