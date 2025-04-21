package io.quarkus.opentelemetry.deployment.metric;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;

public class MetricsEnabled implements BooleanSupplier {
    OTelBuildConfig otelBuildConfig;

    public boolean getAsBoolean() {
        return otelBuildConfig.metrics().enabled()
                .map(new Function<Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean enabled) {
                        return otelBuildConfig.enabled() && enabled;
                    }
                })
                .orElseGet(() -> otelBuildConfig.enabled());
    }
}
