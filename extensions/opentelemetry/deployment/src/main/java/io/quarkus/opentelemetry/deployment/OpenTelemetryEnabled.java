package io.quarkus.opentelemetry.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;

public class OpenTelemetryEnabled implements BooleanSupplier {
    OTelBuildConfig otelConfig;

    @Override
    public boolean getAsBoolean() {
        return otelConfig.enabled();
    }
}
