package io.quarkus.opentelemetry.runtime.exporter.otlp.metrics;

import java.util.ArrayList;
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
 * <p>
 * Known limitation: {@link #getAggregationTemporality(InstrumentType)} only honors the
 * first delegate's preference, since the OpenTelemetry {@link MetricExporter} contract
 * supports only one {@link AggregationTemporality} per {@code MeterProvider} registration.
 * Other delegates' temporality preferences are not applied when composed this way.
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
        // NOTE: OpenTelemetry's MetricExporter contract only supports one AggregationTemporality
        // per MeterProvider registration. When multiple exporters are combined here, only the
        // first delegate's preference is honored; other delegates' preferences are not applied.
        // This is a known limitation of composing exporters this way.
        return delegates.get(0).getAggregationTemporality(instrumentType);
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            results.add(delegate.export(metrics));
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            results.add(delegate.flush());
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            results.add(delegate.shutdown());
        }
        return CompletableResultCode.ofAll(results);
    }
}
