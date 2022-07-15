package io.quarkus.opentelemetry.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.runtime.config.OtelBuildConfig;

public class OpenTelemetryEnabled implements BooleanSupplier {
    OtelBuildConfig otelConfig;

    public boolean getAsBoolean() {
        return otelConfig.enabled();
    }
}
