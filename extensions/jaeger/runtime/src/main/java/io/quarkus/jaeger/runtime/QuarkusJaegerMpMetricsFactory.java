package io.quarkus.jaeger.runtime;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import io.smallrye.metrics.MetricRegistries;

public class QuarkusJaegerMpMetricsFactory implements MetricsFactory {

    Map<MetricID, JaegerGauge> map = new HashMap<>();
    MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);

    /** RUNTIME_INIT from JaegerProcessor */
    QuarkusJaegerMpMetricsFactory() {
        registry.counter(meta("jaeger_tracer_baggage_restrictions_updates", MetricType.COUNTER),
                new Tag("result", "err"));
        registry.counter(meta("jaeger_tracer_baggage_restrictions_updates", MetricType.COUNTER),
                new Tag("result", "ok"));
        registry.counter(meta("jaeger_tracer_baggage_truncations", MetricType.COUNTER));
        registry.counter(meta("jaeger_tracer_baggage_updates", MetricType.COUNTER),
                new Tag("result", "err"));
        registry.counter(meta("jaeger_tracer_baggage_updates", MetricType.COUNTER),
                new Tag("result", "ok"));
        registry.counter(meta("jaeger_tracer_finished_spans", MetricType.COUNTER));
        registry.counter(meta("jaeger_tracer_reporter_spans", MetricType.COUNTER),
                new Tag("result", "dropped"));
        registry.counter(meta("jaeger_tracer_reporter_spans", MetricType.COUNTER),
                new Tag("result", "err"));
        registry.counter(meta("jaeger_tracer_reporter_spans", MetricType.COUNTER),
                new Tag("result", "ok"));
        registry.counter(meta("jaeger_tracer_sampler_queries", MetricType.COUNTER),
                new Tag("result", "err"));
        registry.counter(meta("jaeger_tracer_sampler_queries", MetricType.COUNTER),
                new Tag("result", "ok"));
        registry.counter(meta("jaeger_tracer_sampler_updates", MetricType.COUNTER),
                new Tag("result", "ok"));
        registry.counter(meta("jaeger_tracer_sampler_updates", MetricType.COUNTER),
                new Tag("result", "err"));
        registry.counter(meta("jaeger_tracer_span_context_decoding_errors", MetricType.COUNTER));
        registry.counter(meta("jaeger_tracer_started_spans", MetricType.COUNTER),
                new Tag("sampled", "n"));
        registry.counter(meta("jaeger_tracer_started_spans", MetricType.COUNTER),
                new Tag("sampled", "y"));
        registry.counter(meta("jaeger_tracer_traces", MetricType.COUNTER),
                new Tag("sampled", "y"), new Tag("state", "joined"));
        registry.counter(meta("jaeger_tracer_traces", MetricType.COUNTER),
                new Tag("sampled", "y"), new Tag("state", "started"));
        registry.counter(meta("jaeger_tracer_traces", MetricType.COUNTER),
                new Tag("sampled", "n"), new Tag("state", "joined"));
        registry.counter(meta("jaeger_tracer_traces", MetricType.COUNTER),
                new Tag("sampled", "n"), new Tag("state", "started"));

        registry.register(meta("jaeger_tracer_reporter_queue_length", MetricType.GAUGE),
                new JaegerGauge());
    }

    @Override
    public Counter createCounter(final String name, final Map<String, String> tags) {
        org.eclipse.microprofile.metrics.Counter counter = registry.counter(name, toTagArray(tags));

        return new Counter() {
            @Override
            public void inc(long delta) {
                counter.inc(delta);
            }
        };
    }

    @Override
    public Timer createTimer(final String name, final Map<String, String> tags) {
        org.eclipse.microprofile.metrics.Timer timer = registry.timer(name, toTagArray(tags));
        return new Timer() {
            @Override
            public void durationMicros(long time) {
                timer.update(Duration.of(time, ChronoUnit.MICROS));
            }
        };
    }

    @Override
    public Gauge createGauge(final String name, final Map<String, String> tags) {
        JaegerGauge gauge = getGauge(name, tags);
        return new Gauge() {
            @Override
            public void update(long amount) {
                gauge.update(amount);
            }
        };
    }

    JaegerGauge getGauge(final String name, final Map<String, String> tags) {
        MetricID id = new MetricID(name, toTagArray(tags));
        return map.computeIfAbsent(id, x -> new JaegerGauge());
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
                .build();
    }

    public static class JaegerGauge implements org.eclipse.microprofile.metrics.Gauge<Long> {
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
