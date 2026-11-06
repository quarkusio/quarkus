package io.quarkus.micrometer.runtime.devui;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.quarkus.devui.observability.store.metrics.MetricDistribution;
import io.quarkus.devui.observability.store.metrics.MetricSample;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;

/**
 * Dev-mode-only sampler: on a periodic Vert.x timer it walks the global composite
 * MeterRegistry and records each meter's primary statistic into the shared metrics store, plus
 * the distribution statistics of any timer or summary. Reads run off the request path on the
 * timer thread.
 *
 * Ships in {@code quarkus-micrometer-dev}, a conditional dev dependency, so it is never on
 * a prod/native classpath at all.
 *
 * NOTE: NO class-level scope annotation — registered as a bean only by the dev-only build
 * step (which supplies {@code @Singleton}), so it is not auto-discovered even in dev runs
 * where the capture is suppressed (see {@code MicrometerMetricsDevUIProcessor}).
 */
public class DevUiMetricsSampler {

    // Timers carry no base unit of their own; every duration here is converted to seconds, which
    // is also what the Prometheus registry publishes.
    private static final String SECONDS_UNIT = "s";

    @Inject
    MeterRegistry registry; // resolves to Metrics.globalRegistry (the composite)

    // Produced by MetricsStoreProducer in the quarkus-devui runtime; the type comes from the
    // (minimal) store lib, so this extension depends only on that lib — not on the config.
    @Inject
    MetricsTimeSeriesStore store;

    @Inject
    Vertx vertx;

    private long timerId = -1;

    void onStart(@Observes StartupEvent event) {
        // Sampling interval is carried on the store (set from config by the producer), so the
        // sampler needs no config dependency.
        timerId = vertx.setPeriodic(store.sampleIntervalMillis(), id -> sample());
    }

    @PreDestroy
    void stop() {
        if (timerId >= 0) {
            vertx.cancelTimer(timerId);
            timerId = -1;
        }
    }

    private void sample() {
        long now = System.currentTimeMillis();
        for (Meter meter : registry.getMeters()) {
            MetricSample sample = toSample(meter, now);
            if (sample != null) {
                store.observe(sample);
            }
        }
    }

    private MetricSample toSample(Meter meter, long now) {
        Meter.Id id = meter.getId();
        String name = id.getName();
        String unit = id.getBaseUnit();
        Map<String, String> tags = new LinkedHashMap<>();
        for (Tag t : id.getTags()) {
            tags.put(t.getKey(), t.getValue());
        }
        // Primary statistic per meter type; cumulative flag drives client-side rate. For the
        // distribution types the primary statistic is the recording COUNT, which on its own says
        // nothing about duration or size — the amounts ride along in the MetricDistribution.
        return meter.match(
                gauge -> new MetricSample(name, tags, "GAUGE", false, gauge.value(), now, "micrometer", unit, null),
                counter -> new MetricSample(name, tags, "COUNTER", true, counter.count(), now, "micrometer", unit,
                        null),
                timer -> new MetricSample(name, tags, "TIMER", true, timer.count(), now, "micrometer",
                        SECONDS_UNIT, timerDistribution(timer.takeSnapshot())),
                summary -> new MetricSample(name, tags, "SUMMARY", true, summary.count(), now, "micrometer", unit,
                        summaryDistribution(summary.takeSnapshot())),
                longTaskTimer -> new MetricSample(name, tags, "LONG_TASK_TIMER", false,
                        longTaskTimer.activeTasks(), now, "micrometer", "tasks", null),
                // value() is in the gauge's base time unit, and the id carries no unit of its own; name it
                // so that the reading, and the exported Prometheus name, both say which unit it is in.
                timeGauge -> new MetricSample(name, tags, "GAUGE", false, timeGauge.value(), now, "micrometer",
                        unit == null ? timeGauge.baseTimeUnit().name().toLowerCase(Locale.ROOT) : unit, null),
                functionCounter -> new MetricSample(name, tags, "COUNTER", true,
                        functionCounter.count(), now, "micrometer", unit, null),
                // A FunctionTimer tracks totals only: no max, no percentiles, no histogram.
                functionTimer -> new MetricSample(name, tags, "TIMER", true,
                        functionTimer.count(), now, "micrometer", SECONDS_UNIT,
                        new MetricDistribution(functionTimer.totalTime(TimeUnit.SECONDS), Double.NaN)),
                other -> null);
    }

    /** Durations are reported in seconds throughout, matching what the Prometheus registry exposes. */
    private static MetricDistribution timerDistribution(HistogramSnapshot snap) {
        double[] ranks = new double[snap.percentileValues().length];
        double[] values = new double[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            ValueAtPercentile v = snap.percentileValues()[i];
            ranks[i] = v.percentile();
            values[i] = v.value(TimeUnit.SECONDS);
        }
        CountAtBucket[] buckets = snap.histogramCounts();
        double[] boundaries = new double[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            boundaries[i] = buckets[i].bucket(TimeUnit.SECONDS);
        }
        return new MetricDistribution(snap.total(TimeUnit.SECONDS), snap.max(TimeUnit.SECONDS),
                ranks, values, boundaries, perBucketCounts(buckets, snap.count()));
    }

    private static MetricDistribution summaryDistribution(HistogramSnapshot snap) {
        double[] ranks = new double[snap.percentileValues().length];
        double[] values = new double[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            ValueAtPercentile v = snap.percentileValues()[i];
            ranks[i] = v.percentile();
            values[i] = v.value();
        }
        CountAtBucket[] buckets = snap.histogramCounts();
        double[] boundaries = new double[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            boundaries[i] = buckets[i].bucket();
        }
        return new MetricDistribution(snap.total(), snap.max(), ranks, values, boundaries,
                perBucketCounts(buckets, snap.count()));
    }

    /**
     * Micrometer reports bucket counts as a CDF - each is the number of recordings at or below
     * that bound - while the store wants one count per bucket. De-cumulate, and append the
     * overflow bucket (everything above the last bound), which Micrometer leaves implicit.
     */
    private static double[] perBucketCounts(CountAtBucket[] buckets, long totalCount) {
        if (buckets.length == 0) {
            return null; // no histogram configured on this meter
        }
        double[] counts = new double[buckets.length + 1];
        double previous = 0;
        for (int i = 0; i < buckets.length; i++) {
            counts[i] = Math.max(0, buckets[i].count() - previous);
            previous = buckets[i].count();
        }
        counts[buckets.length] = Math.max(0, totalCount - previous);
        return counts;
    }
}
