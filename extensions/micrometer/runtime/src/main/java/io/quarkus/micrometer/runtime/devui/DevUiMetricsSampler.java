package io.quarkus.micrometer.runtime.devui;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.devui.observability.store.metrics.MetricSample;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;

/**
 * Dev-mode-only sampler: on a periodic Vert.x timer it walks the global composite
 * MeterRegistry and records each meter's primary statistic into the shared metrics store.
 * Reads run off the request path on the timer thread.
 *
 * NOTE: NO class-level scope annotation — registered as a bean only by the dev-only build
 * step (which supplies {@code @Singleton}), so it never exists in prod/native.
 */
public class DevUiMetricsSampler {

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
        Map<String, String> tags = new LinkedHashMap<>();
        for (Tag t : id.getTags()) {
            tags.put(t.getKey(), t.getValue());
        }
        // Primary statistic per meter type; cumulative flag drives client-side rate.
        return meter.match(
                gauge -> new MetricSample(name, tags, "GAUGE", false, gauge.value(), now, "micrometer"),
                counter -> new MetricSample(name, tags, "COUNTER", true, counter.count(), now, "micrometer"),
                timer -> new MetricSample(name, tags, "TIMER", true, timer.count(), now, "micrometer"),
                summary -> new MetricSample(name, tags, "SUMMARY", true, summary.count(), now, "micrometer"),
                longTaskTimer -> new MetricSample(name, tags, "LONG_TASK_TIMER", false,
                        longTaskTimer.activeTasks(), now, "micrometer"),
                timeGauge -> new MetricSample(name, tags, "GAUGE", false, timeGauge.value(), now, "micrometer"),
                functionCounter -> new MetricSample(name, tags, "COUNTER", true,
                        functionCounter.count(), now, "micrometer"),
                functionTimer -> new MetricSample(name, tags, "TIMER", true,
                        functionTimer.count(), now, "micrometer"),
                other -> null);
    }
}
