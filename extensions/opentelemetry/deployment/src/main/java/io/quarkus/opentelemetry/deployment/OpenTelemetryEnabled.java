package io.quarkus.opentelemetry.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;

public class OpenTelemetryEnabled implements BooleanSupplier {
    private final OTelBuildConfig otelConfig;

    public OpenTelemetryEnabled(OTelBuildConfig otelConfig) {
        this.otelConfig = otelConfig;
    }

    public boolean getAsBoolean() {
        return otelConfig.enabled();
    }
}
