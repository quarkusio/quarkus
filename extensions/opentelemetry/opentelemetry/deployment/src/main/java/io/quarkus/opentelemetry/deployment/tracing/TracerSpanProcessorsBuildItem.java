package io.quarkus.opentelemetry.deployment.tracing;

import java.util.List;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class TracerSpanProcessorsBuildItem extends SimpleBuildItem {
    private final RuntimeValue<List<SpanProcessor>> spanProcessors;

    public TracerSpanProcessorsBuildItem(RuntimeValue<List<SpanProcessor>> spanExporters) {
        this.spanProcessors = spanExporters;
    }

    public RuntimeValue<List<SpanProcessor>> getSpanProcessors() {
        return spanProcessors;
    }
}
