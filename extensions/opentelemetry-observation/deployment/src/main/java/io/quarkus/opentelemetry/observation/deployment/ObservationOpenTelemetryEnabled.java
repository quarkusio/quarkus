package io.quarkus.opentelemetry.observation.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.observation.config.ObservationOpenTelemetryConfig;

public class ObservationOpenTelemetryEnabled implements BooleanSupplier {

    ObservationOpenTelemetryConfig config;

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
