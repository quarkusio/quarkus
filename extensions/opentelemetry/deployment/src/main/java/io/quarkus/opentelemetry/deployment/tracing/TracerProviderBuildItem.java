package io.quarkus.opentelemetry.deployment.tracing;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class TracerProviderBuildItem extends SimpleBuildItem {

    private final RuntimeValue<SdkTracerProvider> tracerProvider;

    TracerProviderBuildItem(RuntimeValue<SdkTracerProvider> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    public RuntimeValue<SdkTracerProvider> getTracerProvider() {
        return tracerProvider;
    }
}
