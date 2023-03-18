package io.quarkus.opentelemetry.deployment;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class OpenTelemetryBuildItem extends SimpleBuildItem {

    private final RuntimeValue<OpenTelemetry> value;

    public OpenTelemetryBuildItem(RuntimeValue<OpenTelemetry> value) {
        this.value = value;
    }

    public RuntimeValue<OpenTelemetry> getValue() {
        return value;
    }
}
