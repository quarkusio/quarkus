package io.quarkus.opentelemetry.runtime.devui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.devui.observability.store.metrics.MetricDistribution;
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
        String unit = md.getUnit();
        switch (md.getType()) {
            case LONG_SUM:
                boolean lsMono = md.getLongSumData().isMonotonic();
                for (LongPointData p : md.getLongSumData().getPoints()) {
                    record(name, type, lsMono, unit, p.getAttributes(), p.getValue(), p.getEpochNanos(), null);
                }
                break;
            case DOUBLE_SUM:
                boolean dsMono = md.getDoubleSumData().isMonotonic();
                for (DoublePointData p : md.getDoubleSumData().getPoints()) {
                    record(name, type, dsMono, unit, p.getAttributes(), p.getValue(), p.getEpochNanos(), null);
                }
                break;
            case LONG_GAUGE:
                for (LongPointData p : md.getLongGaugeData().getPoints()) {
                    record(name, type, false, unit, p.getAttributes(), p.getValue(), p.getEpochNanos(), null);
                }
                break;
            case DOUBLE_GAUGE:
                for (DoublePointData p : md.getDoubleGaugeData().getPoints()) {
                    record(name, type, false, unit, p.getAttributes(), p.getValue(), p.getEpochNanos(), null);
                }
                break;
            case HISTOGRAM:
                // The primary value is the recording count; the sum, max and buckets that say what
                // was actually measured travel in the MetricDistribution.
                for (HistogramPointData p : md.getHistogramData().getPoints()) {
                    record(name, type, true, unit, p.getAttributes(), p.getCount(), p.getEpochNanos(),
                            distribution(p));
                }
                break;
            default:
                // EXPONENTIAL_HISTOGRAM, SUMMARY: not charted in the POC. An exponential histogram
                // needs its buckets reconstructed from scale + offset before it can be drawn.
                break;
        }
    }

    /** Boundaries and per-bucket counts come straight through: this is the shape the store wants. */
    private static MetricDistribution distribution(HistogramPointData p) {
        List<Double> boundaries = p.getBoundaries();
        List<Long> counts = p.getCounts();
        double[] bounds = new double[boundaries.size()];
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = boundaries.get(i);
        }
        double[] perBucket = new double[counts.size()];
        for (int i = 0; i < perBucket.length; i++) {
            perBucket[i] = counts.get(i);
        }
        // OpenTelemetry only tracks a max when the aggregation records one.
        double max = p.hasMax() ? p.getMax() : Double.NaN;
        return new MetricDistribution(p.getSum(), max, null, null, bounds, perBucket);
    }

    private void record(String name, String type, boolean cumulative, String unit, Attributes attrs,
            double value, long epochNanos, MetricDistribution distribution) {
        Map<String, String> tags = new LinkedHashMap<>();
        attrs.forEach((k, v) -> tags.put(k.getKey(), String.valueOf(v)));
        store.observe(new MetricSample(name, tags, type, cumulative, value, epochNanos / 1_000_000L, "otel",
                unit, distribution));
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
