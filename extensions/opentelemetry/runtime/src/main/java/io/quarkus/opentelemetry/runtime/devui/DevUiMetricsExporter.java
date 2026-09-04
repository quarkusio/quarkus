package io.quarkus.opentelemetry.runtime.devui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.devui.observability.store.metrics.MetricSample;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;

/**
 * Dev-mode-only, in-memory OpenTelemetry MetricExporter that converts each collected
 * MetricData point into a {@link MetricSample} and records it into the shared store.
 * Wrapped in a PeriodicMetricReader by {@link DevUiMetricsSdkBuilderCustomizer}. Reports
 * CUMULATIVE temporality so counters/sums arrive as running totals (the client derives the
 * per-interval rate). This is LOAD-BEARING: if the exporter defaulted to DELTA, each export
 * would already be a per-interval delta and the client would difference an already-differenced
 * series -> wrong rates and a lying {@code cumulative} flag. Each MetricReader keeps its own
 * last-collection state, so running this alongside the OTLP PeriodicMetricReader is safe.
 */
public class DevUiMetricsExporter implements MetricExporter {

    private final MetricsTimeSeriesStore store;

    public DevUiMetricsExporter(MetricsTimeSeriesStore store) {
        this.store = store;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        for (MetricData md : metrics) {
            convert(md);
        }
        return CompletableResultCode.ofSuccess();
    }

    private void convert(MetricData md) {
        String name = md.getName();
        String type = md.getType().name();
        switch (md.getType()) {
            case LONG_SUM:
                boolean lsMono = md.getLongSumData().isMonotonic();
                for (LongPointData p : md.getLongSumData().getPoints()) {
                    record(name, type, lsMono, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case DOUBLE_SUM:
                boolean dsMono = md.getDoubleSumData().isMonotonic();
                for (DoublePointData p : md.getDoubleSumData().getPoints()) {
                    record(name, type, dsMono, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case LONG_GAUGE:
                for (LongPointData p : md.getLongGaugeData().getPoints()) {
                    record(name, type, false, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case DOUBLE_GAUGE:
                for (DoublePointData p : md.getDoubleGaugeData().getPoints()) {
                    record(name, type, false, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case HISTOGRAM:
                for (var p : md.getHistogramData().getPoints()) {
                    record(name, type, true, p.getAttributes(), p.getCount(), p.getEpochNanos());
                }
                break;
            default:
                // EXPONENTIAL_HISTOGRAM, SUMMARY: not charted in the POC.
                break;
        }
    }

    private void record(String name, String type, boolean cumulative, Attributes attrs,
            double value, long epochNanos) {
        Map<String, String> tags = new LinkedHashMap<>();
        attrs.forEach((k, v) -> tags.put(k.getKey(), String.valueOf(v)));
        store.observe(new MetricSample(name, tags, type, cumulative, value, epochNanos / 1_000_000L, "otel"));
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
