package io.quarkus.jaeger.runtime;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import io.smallrye.metrics.MetricRegistries;

public class QuarkusJaegerMetricsFactory implements MetricsFactory {

    MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);

    @Override
    public Counter createCounter(final String name, final Map<String, String> tags) {
        org.eclipse.microprofile.metrics.Counter counter = registry.counter(meta(name, MetricType.COUNTER), toTagArray(tags));

        return new Counter() {
            @Override
            public void inc(long delta) {
                counter.inc(delta);
            }
        };
    }

    @Override
    public Timer createTimer(final String name, final Map<String, String> tags) {
        org.eclipse.microprofile.metrics.Timer timer = registry.timer(meta(name, MetricType.TIMER), toTagArray(tags));

        return new Timer() {
            @Override
            public void durationMicros(long time) {
                timer.update(time, TimeUnit.MICROSECONDS);
            }
        };
    }

    @Override
    public Gauge createGauge(final String name, final Map<String, String> tags) {
        JaegerGauge gauge = registry.register(meta(name, MetricType.GAUGE), new JaegerGauge(), toTagArray(tags));

        return new Gauge() {
            @Override
            public void update(long amount) {
                gauge.update(amount);
            }
        };
    }

    private Tag[] toTagArray(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(entry -> new Tag(entry.getKey(), entry.getValue()))
                .toArray(Tag[]::new);
    }

    static Metadata meta(String name, MetricType type) {
        return Metadata.builder()
                .withName(name)
                .withDisplayName(name)
                .withType(type)
                .withUnit("none")
                .withDescription(name)
                .reusable()
                .build();
    }

    static class JaegerGauge implements org.eclipse.microprofile.metrics.Gauge<Long> {
        private AtomicLong value = new AtomicLong();

        public void update(long value) {
            this.value.set(value);
        }

        @Override
        public Long getValue() {
            return value.get();
        }
    }
}
