package io.quarkus.micrometer.runtime.devui;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.quarkus.dev.telemetry.MetricEventBuilder;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetryEvents;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;

/**
 * Dev-mode-only sampler: on a periodic Vert.x timer it walks the global composite
 * MeterRegistry and sends each meter's primary statistic to the Dev UI as a {@code metric}
 * telemetry event, plus the distribution statistics of any timer or summary. Reads run off the
 * request path on the timer thread. It knows nothing of how the readings are kept or shown.
 *
 * Ships in {@code quarkus-micrometer-dev}, a conditional dev dependency, so it is never on
 * a prod/native classpath at all.
 *
 * NOTE: NO class-level scope annotation — registered as a bean only by the dev-only build
 * step (which supplies {@code @Singleton}), so it is not auto-discovered even in dev runs
 * where the capture is suppressed (see {@code MicrometerMetricsDevUIProcessor}).
 */
public class DevUiMetricsSampler {

    private static final Logger LOG = Logger.getLogger(DevUiMetricsSampler.class);

    // Timers carry no base unit of their own; every duration here is converted to seconds, which
    // is also what the Prometheus registry publishes.
    private static final String SECONDS_UNIT = "s";

    @Inject
    MeterRegistry registry; // resolves to Metrics.globalRegistry (the composite)

    // Received by the Dev UI, which is not a dependency of this extension: the event type comes
    // from quarkus-core's dev mode SPI.
    @Inject
    Event<TelemetryEvent> telemetry;

    @Inject
    Vertx vertx;

    private long timerId = -1;

    void onStart(@Observes StartupEvent event) {
        // The interval the Dev UI dashboard samples at, read from config: this extension does
        // not depend on Dev UI.
        Duration interval = TelemetryEvents.metricsSampleInterval(ConfigProvider.getConfig()::getOptionalValue);
        timerId = vertx.setPeriodic(interval.toMillis(), id -> sample());
    }

    @PreDestroy
    void stop() {
        if (timerId >= 0) {
            vertx.cancelTimer(timerId);
            timerId = -1;
        }
    }

    void sample() {
        if (!TelemetryEvents.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Meter meter : registry.getMeters()) {
            // One meter that cannot be sent must not cost the others their reading, on this and every tick.
            try {
                TelemetryEvent event = toEvent(meter, now);
                if (event != null) {
                    telemetry.fire(event);
                }
            } catch (RuntimeException e) {
                LOG.debugf(e, "Could not send meter %s to the Dev UI", meter.getId().getName());
            }
        }
    }

    private TelemetryEvent toEvent(Meter meter, long now) {
        Meter.Id id = meter.getId();
        String name = id.getName();
        if (name == null || name.isBlank()) {
            return null; // nothing to chart it under
        }
        String unit = id.getBaseUnit();
        Map<String, String> tags = new LinkedHashMap<>();
        for (Tag t : id.getTags()) {
            tags.put(t.getKey(), t.getValue());
        }
        // Primary statistic per meter type; cumulative flag drives client-side rate. For the
        // distribution types the primary statistic is the recording COUNT, which on its own says
        // nothing about duration or size — the amounts ride along as the event's distribution.
        return meter.match(
                gauge -> metric(name, tags, now, "GAUGE", false, unit).value(gauge.value()).build(),
                counter -> metric(name, tags, now, "COUNTER", true, unit).value(counter.count()).build(),
                timer -> timerDistribution(metric(name, tags, now, "TIMER", true, SECONDS_UNIT)
                        .value(timer.count()), timer.takeSnapshot()).build(),
                summary -> summaryDistribution(metric(name, tags, now, "SUMMARY", true, unit)
                        .value(summary.count()), summary.takeSnapshot()).build(),
                longTaskTimer -> metric(name, tags, now, "LONG_TASK_TIMER", false, "tasks")
                        .value(longTaskTimer.activeTasks()).build(),
                timeGauge -> metric(name, tags, now, "GAUGE", false, unit).value(timeGauge.value()).build(),
                functionCounter -> metric(name, tags, now, "COUNTER", true, unit)
                        .value(functionCounter.count()).build(),
                // A FunctionTimer tracks totals only: no max, no percentiles, no histogram.
                functionTimer -> metric(name, tags, now, "TIMER", true, SECONDS_UNIT)
                        .value(functionTimer.count())
                        .distribution(functionTimer.totalTime(TimeUnit.SECONDS), Double.NaN).build(),
                other -> null);
    }

    private static MetricEventBuilder metric(String name, Map<String, String> tags, long now, String type,
            boolean cumulative, String unit) {
        return TelemetryEvent.metric(name)
                .type(type)
                .cumulative(cumulative)
                .unit(unit)
                .tags(tags)
                .source("micrometer")
                .timestamp(now);
    }

    /** Durations are reported in seconds throughout, matching what the Prometheus registry exposes. */
    private static MetricEventBuilder timerDistribution(MetricEventBuilder event, HistogramSnapshot snap) {
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
        return event.distribution(snap.total(TimeUnit.SECONDS), snap.max(TimeUnit.SECONDS))
                .percentiles(ranks, values)
                .buckets(boundaries, perBucketCounts(buckets, snap.count()));
    }

    private static MetricEventBuilder summaryDistribution(MetricEventBuilder event, HistogramSnapshot snap) {
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
        return event.distribution(snap.total(), snap.max())
                .percentiles(ranks, values)
                .buckets(boundaries, perBucketCounts(buckets, snap.count()));
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
