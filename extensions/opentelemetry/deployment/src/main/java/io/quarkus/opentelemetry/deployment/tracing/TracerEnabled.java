package io.quarkus.opentelemetry.deployment.tracing;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import io.quarkus.opentelemetry.runtime.config.build.OtelBuildConfig;

public class TracerEnabled implements BooleanSupplier {
    OtelBuildConfig otelConfig;

    public boolean getAsBoolean() {
        return otelConfig.traces.enabled.map(new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(Boolean tracerEnabled) {
                return otelConfig.enabled && tracerEnabled;
            }
        })
                .orElseGet(() -> otelConfig.enabled);
    }
}
