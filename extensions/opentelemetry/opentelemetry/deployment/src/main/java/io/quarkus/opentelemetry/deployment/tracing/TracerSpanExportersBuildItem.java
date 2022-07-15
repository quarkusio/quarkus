package io.quarkus.opentelemetry.deployment.tracing;

import java.util.List;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class TracerSpanExportersBuildItem extends SimpleBuildItem {
    private final RuntimeValue<List<SpanExporter>> spanExporters;

    public TracerSpanExportersBuildItem(RuntimeValue<List<SpanExporter>> spanExporters) {
        this.spanExporters = spanExporters;
    }

    public RuntimeValue<List<SpanExporter>> getSpanExporters() {
        return spanExporters;
    }
}
