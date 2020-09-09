package io.quarkus.micrometer.deployment;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class RootMeterRegistryBuildItem extends SimpleBuildItem {
    private final RuntimeValue<MeterRegistry> value;

    public RootMeterRegistryBuildItem(RuntimeValue<MeterRegistry> value) {
        this.value = value;
    }

    public RuntimeValue<MeterRegistry> getValue() {
        return value;
    }
}
