package io.quarkus.arc.deployment;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class OpenTelemetrySdkBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Boolean> runtimeEnabled;

    public OpenTelemetrySdkBuildItem(RuntimeValue<Boolean> sdkEnabled) {
        this.runtimeEnabled = sdkEnabled;
    }

    /**
     * True if the OpenTelemetry SDK is enabled at build and runtime.
     */
    public RuntimeValue<Boolean> isRuntimeEnabled() {
        return runtimeEnabled;
    }

    public static Optional<RuntimeValue<Boolean>> isOtelSdkEnabled(Optional<OpenTelemetrySdkBuildItem> buildItem) {
        // optional is empty if the extension is disabled at build time
        return buildItem.isPresent() ? Optional.of(buildItem.get().isRuntimeEnabled()) : Optional.empty();
    }
}
