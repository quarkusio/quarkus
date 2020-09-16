package io.quarkus.jaeger.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

public class QuarkusJaegerMicrometerFactory implements MetricsFactory {
    MeterRegistry registry = Metrics.globalRegistry;

    @Override
    public Counter createCounter(String name, Map<String, String> tags) {
        return new Counter() {
            private final io.micrometer.core.instrument.Counter counter = registry.counter(name, translateTags(tags));

            @Override
            public void inc(long amount) {
                counter.increment(amount);
            }
        };
    }

    @Override
    public Timer createTimer(String name, Map<String, String> tags) {
        return new Timer() {
            private final io.micrometer.core.instrument.Timer timer = registry.timer(name, translateTags(tags));

            @Override
            public void durationMicros(long amount) {
                timer.record(amount, TimeUnit.MICROSECONDS);
            }
        };
    }

    @Override
    public Gauge createGauge(String name, Map<String, String> tags) {
        return new Gauge() {
            private final Iterable<Tag> tagList = translateTags(tags);

            @Override
            public void update(long amount) {
                registry.gauge(name, tagList, amount);
            }
        };
    }

    private Iterable<Tag> translateTags(Map<String, String> tags) {
        final List<Tag> tagList = new ArrayList<Tag>(tags.size());
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            tagList.add(Tag.of(tag.getKey(), tag.getValue()));
        }
        return tagList;
    }
}
