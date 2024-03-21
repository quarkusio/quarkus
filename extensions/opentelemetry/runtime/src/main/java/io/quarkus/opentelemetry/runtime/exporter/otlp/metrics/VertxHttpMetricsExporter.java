package io.quarkus.opentelemetry.runtime.exporter.otlp.metrics;

import java.util.Collection;

import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class VertxHttpMetricsExporter implements MetricExporter {

    private final HttpExporter<MetricsRequestMarshaler> delegate;

    public VertxHttpMetricsExporter(HttpExporter<MetricsRequestMarshaler> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        return delegate.export(MetricsRequestMarshaler.create(metrics), metrics.size());
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE; // FIXME Make configurable
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return Aggregation.defaultAggregation(); // FIXME Make configurable
    }

    @Override
    public MemoryMode getMemoryMode() {
        return MemoryMode.IMMUTABLE_DATA; // FIXME Make configurable
    }
}
