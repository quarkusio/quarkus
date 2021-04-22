package io.quarkus.jaeger.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.jaeger.runtime.JaegerBuildTimeConfig;

public class JaegerEnabled implements BooleanSupplier {

    private final JaegerBuildTimeConfig buildTimeConfig;

    public JaegerEnabled(JaegerBuildTimeConfig buildTimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return buildTimeConfig.enabled;
    }

}
