package io.quarkus.smallrye.metrics.runtime;

import io.quarkus.runtime.metrics.MetricsFactory;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class SmallRyeMetricsFactory implements MetricsFactory {

    @Override
    public MetricBuilder builder(String name, MetricsFactory.Type type) {
        return new SmallRyeMetricBuilder(name, type);
    }

    static class SmallRyeMetricBuilder implements MetricsFactory.MetricBuilder {
        final MetricRegistry registry;
        final org.eclipse.microprofile.metrics.MetadataBuilder builder;
        final List<Tag> tags = new ArrayList<>();

        SmallRyeMetricBuilder(String name, MetricsFactory.Type type) {
            switch (type) {
                case APPLICATION:
                    registry = MetricRegistries.get(MetricRegistry.Type.APPLICATION);
                    break;
                case BASE:
                    registry = MetricRegistries.get(MetricRegistry.Type.BASE);
                    break;
                default:
                    registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
                    break;
            }
            builder = org.eclipse.microprofile.metrics.Metadata.builder()
                    .withName(name);
        }

        public MetricBuilder unit(String unit) {
            builder.withUnit(unit);
            return this;
        }

        @Override
        public MetricBuilder tag(String key, String value) {
            tags.add(new Tag(key, value));
            return this;
        }

        @Override
        public MetricBuilder description(String description) {
            builder.withDescription(description);
            return this;
        }

        @Override
        public void buildCounter(Supplier<Number> countFunction) {
            builder.withType(MetricType.COUNTER);
            registry.register(builder.build(), new SmallRyeCounter(countFunction), tags.toArray(new Tag[0]));
        }

        @Override
        public <T, R extends Number> void buildCounter(T obj, Function<T, R> countFunction) {
            builder.withType(MetricType.COUNTER);
            registry.register(builder.build(), new SmallRyeFunctionCounter<>(obj, countFunction), tags.toArray(new Tag[0]));
        }

        @Override
        public void buildGauge(Supplier<Number> gaugeFunction) {
            builder.withType(MetricType.GAUGE);
            registry.register(builder.build(), new SmallRyeGauge(gaugeFunction), tags.toArray(new Tag[0]));
        }

        @Override
        public <T, R extends Number> void buildGauge(T obj, Function<T, R> gaugeFunction) {
            builder.withType(MetricType.GAUGE);
            registry.register(builder.build(), new SmallRyeFunctionGauge<>(obj, gaugeFunction), tags.toArray(new Tag[0]));
        }

        @Override
        public TimeRecorder buildTimer() {
            builder.withType(MetricType.SIMPLE_TIMER);
            SimpleTimer timer = registry.simpleTimer(builder.build(), tags.toArray(new Tag[0]));
            return new SmallRyeTimeRecorder(timer);
        }

        @Override
        public Runnable buildTimer(Runnable f) {
            builder.withType(MetricType.SIMPLE_TIMER);
            return () -> registry.simpleTimer(builder.build(), tags.toArray(new Tag[0])).time(f);
        }

        @Override
        public <T> Callable<T> buildTimer(Callable<T> f) {
            builder.withType(MetricType.SIMPLE_TIMER);
            return () -> registry.simpleTimer(builder.build(), tags.toArray(new Tag[0])).time(f);
        }

        @Override
        public <T> Supplier<T> buildTimer(Supplier<T> f) {
            builder.withType(MetricType.SIMPLE_TIMER);
            return () -> {
                try {
                    return f.get();
                } finally {
                    registry.simpleTimer(builder.build(), tags.toArray(new Tag[0])).time().stop();
                }
            };
        }
    }

    private record SmallRyeCounter(Supplier<Number> f) implements Counter {

        @Override
        public void inc() {
        }

        @Override
        public void inc(long l) {
        }

        @Override
        public long getCount() {
            return f.get().longValue();
        }
    }

    private record SmallRyeFunctionCounter<T, R extends Number>(T obj, Function<T, R> f) implements Counter {

        @Override
        public void inc() {
        }

        @Override
        public void inc(long l) {
        }

        @Override
        public long getCount() {
            return f.apply(obj).longValue();
        }
    }

    private record SmallRyeGauge(Supplier<Number> f) implements Gauge<Long> {

        @Override
        public Long getValue() {
            return f.get().longValue();
        }
    }

    private record SmallRyeFunctionGauge<T, R extends Number>(T obj, Function<T, R> f) implements Gauge<R> {

        @Override
        public R getValue() {
            return f.apply(obj);
        }
    }

    private static class SmallRyeTimeRecorder implements TimeRecorder {
        SimpleTimer timer;

        SmallRyeTimeRecorder(SimpleTimer timer) {
            this.timer = timer;
        }

        @Override
        public void update(Duration duration) {
            timer.update(duration);
        }

        @Override
        public void update(long duration, TimeUnit unit) {
            timer.update(Duration.ofNanos(unit.toNanos(duration)));
        }
    }
}
