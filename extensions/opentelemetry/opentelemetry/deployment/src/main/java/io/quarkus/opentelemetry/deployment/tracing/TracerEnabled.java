package io.quarkus.opentelemetry.deployment.tracing;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.runtime.config.OtelBuildConfig;

public class TracerEnabled implements BooleanSupplier {
    OtelBuildConfig otelConfig;

    public boolean getAsBoolean() {
        return otelConfig.traces().enabled()
                .map(tracerEnabled -> otelConfig.enabled() && tracerEnabled)
                .orElseGet(() -> otelConfig.enabled());
    }
}
