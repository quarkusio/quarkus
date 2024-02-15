package io.quarkus.opentelemetry.runtime.exporter.otlp.metrics;

import java.util.Collection;

import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class VertxGrpcMetricExporter implements MetricExporter {

    private final GrpcExporter<MetricsRequestMarshaler> delegate;

    public VertxGrpcMetricExporter(GrpcExporter<MetricsRequestMarshaler> grpcExporter) {
        this.delegate = grpcExporter;
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
