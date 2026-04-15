package io.quarkus.opentelemetry.runtime.exporter.otlp.metrics;

import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

/**
 * A {@link MetricExporter} that delegates to multiple {@link MetricExporter} instances,
 * exporting to all of them. Used to allow the default OTLP exporter to coexist with
 * user-defined exporters registered via CDI.
 */
public final class CompositeMetricExporter implements MetricExporter {

    private final List<MetricExporter> delegates;

    private CompositeMetricExporter(List<MetricExporter> delegates) {
        this.delegates = delegates;
    }

    public static MetricExporter of(Collection<MetricExporter> exporters) {
        List<MetricExporter> delegates = List.copyOf(exporters);
        if (delegates.isEmpty()) {
            return NoopMetricExporter.INSTANCE;
        }
        if (delegates.size() == 1) {
            return delegates.get(0);
        }
        return new CompositeMetricExporter(delegates);
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        // Use the first delegate's preference; OTel does not support per-exporter temporality
        // negotiation for a single MeterProvider registration.
        return delegates.get(0).getAggregationTemporality(instrumentType);
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        List<CompletableResultCode> results = delegates.stream()
                .map(delegate -> delegate.export(metrics))
                .toList();
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = delegates.stream()
                .map(MetricExporter::flush)
                .toList();
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = delegates.stream()
                .map(MetricExporter::shutdown)
                .toList();
        return CompletableResultCode.ofAll(results);
    }
}
