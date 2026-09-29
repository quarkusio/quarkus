package io.quarkus.dev.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link TelemetrySignals#METRIC metric} event: one reading of a meter, with its distribution when it has
 * one. Obtained from {@link TelemetryEvent#metric(String)}.
 * <p>
 * Readings that are not finite (a gauge with nothing to report, a distribution without a maximum) are left out rather
 * than carried as {@code NaN}, which JSON cannot represent; a meter without a finite value is not charted.
 * <p>
 * Everything the builder puts in the event is already immutable and JSON compatible, so the event takes it without
 * copying or checking it again. The tags are copied when they are set.
 */
public final class MetricEventBuilder {

    private final String name;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private long timestamp = System.currentTimeMillis();

    MetricEventBuilder(String name) {
        this.name = name;
    }

    /** The primary statistic: the reading, or the recording count of a distribution. */
    public MetricEventBuilder value(double value) {
        putFinite(TelemetryAttributes.VALUE, value);
        return this;
    }

    /** The kind of meter, e.g. {@code GAUGE} (the default), {@code COUNTER} or {@code TIMER}. */
    public MetricEventBuilder type(String type) {
        attributes.put(TelemetryAttributes.TYPE, type);
        return this;
    }

    /** Whether the value is a running total, charted as how fast it grows. */
    public MetricEventBuilder cumulative(boolean cumulative) {
        attributes.put(TelemetryAttributes.CUMULATIVE, cumulative);
        return this;
    }

    public MetricEventBuilder unit(String unit) {
        if (unit != null) {
            attributes.put(TelemetryAttributes.UNIT, unit);
        }
        return this;
    }

    public MetricEventBuilder tags(Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            attributes.put(TelemetryAttributes.TAGS, CheckedAttributes.strings(tags));
        }
        return this;
    }

    public MetricEventBuilder source(String source) {
        attributes.put(TelemetryAttributes.SOURCE, source);
        return this;
    }

    /** When the reading was taken, in milliseconds since the epoch; defaults to when the builder was created. */
    public MetricEventBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /** The sum and maximum of what a distribution recorded; a maximum that is not finite is left out. */
    public MetricEventBuilder distribution(double total, double max) {
        putFinite(TelemetryAttributes.TOTAL, total);
        putFinite(TelemetryAttributes.MAX, max);
        return this;
    }

    /** The percentiles of a distribution, as parallel arrays of ranks (0 to 1) and values. */
    public MetricEventBuilder percentiles(double[] ranks, double[] values) {
        if (ranks != null && values != null && ranks.length > 0 && ranks.length == values.length) {
            List<Map<String, Object>> percentiles = new ArrayList<>(ranks.length);
            for (int i = 0; i < ranks.length; i++) {
                Map<String, Object> percentile = new LinkedHashMap<>(4);
                percentile.put("rank", finiteOrNull(ranks[i]));
                percentile.put("value", finiteOrNull(values[i]));
                percentiles.add(Collections.unmodifiableMap(percentile));
            }
            attributes.put(TelemetryAttributes.PERCENTILES, Collections.unmodifiableList(percentiles));
        }
        return this;
    }

    /**
     * A distribution's histogram: the buckets' upper bounds, and the recordings in each bucket - one more count than
     * boundaries, the last being above them all.
     */
    public MetricEventBuilder buckets(double[] boundaries, double[] counts) {
        if (boundaries != null && counts != null && boundaries.length > 0) {
            attributes.put(TelemetryAttributes.BUCKET_BOUNDARIES, list(boundaries));
            attributes.put(TelemetryAttributes.BUCKET_COUNTS, list(counts));
        }
        return this;
    }

    public TelemetryEvent build() {
        // A copy, so that the builder can go on without changing the event it built.
        return new TelemetryEvent(TelemetrySignals.METRIC, name, timestamp,
                new CheckedAttributes(new LinkedHashMap<>(attributes)));
    }

    private void putFinite(String key, double value) {
        if (Double.isFinite(value)) {
            attributes.put(key, value);
        }
    }

    private static Double finiteOrNull(double value) {
        return Double.isFinite(value) ? value : null;
    }

    private static List<Double> list(double[] values) {
        List<Double> list = new ArrayList<>(values.length);
        for (double value : values) {
            list.add(finiteOrNull(value));
        }
        return Collections.unmodifiableList(list);
    }
}
