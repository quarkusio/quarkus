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
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class VertxHttpMetricsExporter implements MetricExporter {

    private final HttpExporter<MetricsRequestMarshaler> delegate;
    private final AggregationTemporalitySelector aggregationTemporalitySelector;
    private final DefaultAggregationSelector defaultAggregationSelector;

    public VertxHttpMetricsExporter(HttpExporter<MetricsRequestMarshaler> delegate,
            AggregationTemporalitySelector aggregationTemporalitySelector,
            DefaultAggregationSelector defaultAggregationSelector) {
        this.delegate = delegate;
        this.aggregationTemporalitySelector = aggregationTemporalitySelector;
        this.defaultAggregationSelector = defaultAggregationSelector;
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
        return this.aggregationTemporalitySelector.getAggregationTemporality(instrumentType);
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return defaultAggregationSelector.getDefaultAggregation(instrumentType);
    }

    @Override
    public MemoryMode getMemoryMode() {
        return MemoryMode.IMMUTABLE_DATA; // Same as the default in the OTLP exporter
    }
}
