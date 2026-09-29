package io.quarkus.opentelemetry.runtime.devui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.dev.telemetry.MetricEventBuilder;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetryEvents;

/**
 * Dev-mode-only OpenTelemetry MetricExporter that sends each collected MetricData point to the
 * Dev UI as a {@code metric} telemetry event. It knows nothing of how the readings are kept or shown.
 * Wrapped in a PeriodicMetricReader by {@link DevUiMetricsSdkBuilderCustomizer}. Reports
 * CUMULATIVE temporality so counters/sums arrive as running totals (the client derives the
 * per-interval rate). This is LOAD-BEARING: if the exporter defaulted to DELTA, each export
 * would already be a per-interval delta and the client would difference an already-differenced
 * series -> wrong rates and a lying {@code cumulative} flag. Each MetricReader keeps its own
 * last-collection state, so running this alongside the OTLP PeriodicMetricReader is safe.
 */
public class DevUiMetricsExporter implements MetricExporter {

    private static final Logger LOG = Logger.getLogger(DevUiMetricsExporter.class);

    private final Event<TelemetryEvent> telemetry;

    public DevUiMetricsExporter(Event<TelemetryEvent> telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (!TelemetryEvents.isEnabled()) {
            return CompletableResultCode.ofSuccess();
        }
        for (MetricData md : metrics) {
            // One meter that cannot be sent must not cost the others their reading, on this and every export.
            try {
                convert(md);
            } catch (RuntimeException e) {
                LOG.debugf(e, "Could not send metric %s to the Dev UI", md.getName());
            }
        }
        return CompletableResultCode.ofSuccess();
    }

    private void convert(MetricData md) {
        String name = md.getName();
        if (name == null || name.isBlank()) {
            return; // nothing to chart it under
        }
        String type = md.getType().name();
        String unit = md.getUnit();
        switch (md.getType()) {
            case LONG_SUM:
                boolean lsMono = md.getLongSumData().isMonotonic();
                for (LongPointData p : md.getLongSumData().getPoints()) {
                    record(name, type, lsMono, unit, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case DOUBLE_SUM:
                boolean dsMono = md.getDoubleSumData().isMonotonic();
                for (DoublePointData p : md.getDoubleSumData().getPoints()) {
                    record(name, type, dsMono, unit, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case LONG_GAUGE:
                for (LongPointData p : md.getLongGaugeData().getPoints()) {
                    record(name, type, false, unit, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case DOUBLE_GAUGE:
                for (DoublePointData p : md.getDoubleGaugeData().getPoints()) {
                    record(name, type, false, unit, p.getAttributes(), p.getValue(), p.getEpochNanos());
                }
                break;
            case HISTOGRAM:
                // The primary value is the recording count; the sum, max and buckets that say what
                // was actually measured travel as the event's distribution.
                for (HistogramPointData p : md.getHistogramData().getPoints()) {
                    MetricEventBuilder event = event(name, type, true, unit, p.getAttributes(), p.getCount(),
                            p.getEpochNanos());
                    distribution(event, p);
                    telemetry.fire(event.build());
                }
                break;
            default:
                // EXPONENTIAL_HISTOGRAM, SUMMARY: not charted in the POC. An exponential histogram
                // needs its buckets reconstructed from scale + offset before it can be drawn.
                break;
        }
    }

    /** Boundaries and per-bucket counts come straight through: this is the shape the store wants. */
    private static void distribution(MetricEventBuilder event, HistogramPointData p) {
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
        event.distribution(p.getSum(), p.hasMax() ? p.getMax() : Double.NaN)
                .buckets(bounds, perBucket);
    }

    private void record(String name, String type, boolean cumulative, String unit, Attributes attrs,
            double value, long epochNanos) {
        telemetry.fire(event(name, type, cumulative, unit, attrs, value, epochNanos).build());
    }

    private static MetricEventBuilder event(String name, String type, boolean cumulative, String unit,
            Attributes attrs, double value, long epochNanos) {
        Map<String, String> tags = new LinkedHashMap<>();
        attrs.forEach((k, v) -> tags.put(k.getKey(), String.valueOf(v)));
        return TelemetryEvent.metric(name)
                .value(value)
                .type(type)
                .cumulative(cumulative)
                .unit(unit)
                .tags(tags)
                .source("otel")
                .timestamp(epochNanos / 1_000_000L);
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
