package io.quarkus.opentelemetry.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.runtime.config.OpenTelemetryConfig;

public class OpenTelemetryEnabled implements BooleanSupplier {
    OpenTelemetryConfig otelConfig;

    public boolean getAsBoolean() {
        return otelConfig.enabled;
    }
}
