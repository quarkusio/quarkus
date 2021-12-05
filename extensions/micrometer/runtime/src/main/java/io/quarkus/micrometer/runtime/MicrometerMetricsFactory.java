package io.quarkus.micrometer.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.runtime.metrics.MetricsFactory;

public class MicrometerMetricsFactory implements MetricsFactory {
    final MeterRegistry globalRegistry;
    final MicrometerConfig config;

    public MicrometerMetricsFactory(MicrometerConfig config, MeterRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
        this.config = config;
    }

    @Override
    public boolean metricsSystemSupported(String name) {
        return MetricsFactory.MICROMETER.equals(name) ||
                (MetricsFactory.MP_METRICS.equals(name) && config.binder.mpMetrics.enabled.orElse(false));
    }

    /**
     * @param name The name of the metric (required)
     * @param type The scope or type of the metric (ignored)
     * @return a fluid builder for registering metrics.
     */
    @Override
    public MetricBuilder builder(String name, MetricsFactory.Type type) {
        return new MicrometerMetricsBuilder(name);
    }

    class MicrometerMetricsBuilder implements MetricBuilder {
        String name;
        String description;
        String unit;
        Tags tags = Tags.empty();

        public MicrometerMetricsBuilder(String name) {
            this.name = name;
        }

        @Override
        public MetricBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public MetricBuilder tag(String key, String value) {
            this.tags = tags.and(key, value);
            return this;
        }

        @Override
        public MetricBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public void buildCounter(Supplier<Number> countFunction) {
            FunctionCounter.builder(name, countFunction, x -> countFunction.get().doubleValue())
                    .description(description)
                    .baseUnit(unit)
                    .tags(tags)
                    .register(globalRegistry);
        }

        @Override
        public <T, R extends Number> void buildCounter(T obj, Function<T, R> countFunction) {
            FunctionCounter.builder(name, obj, x -> countFunction.apply(x).doubleValue())
                    .description(description)
                    .baseUnit(unit)
                    .tags(tags)
                    .register(globalRegistry);
        }

        @Override
        public void buildGauge(Supplier<Number> gaugeFunction) {
            Gauge.builder(name, gaugeFunction)
                    .description(description)
                    .baseUnit(unit)
                    .tags(tags)
                    .strongReference(true)
                    .register(globalRegistry);
        }

        @Override
        public <T, R extends Number> void buildGauge(T obj, Function<T, R> gaugeFunction) {
            Gauge.builder(name, obj, x -> gaugeFunction.apply(obj).doubleValue())
                    .description(description)
                    .baseUnit(unit)
                    .tags(tags)
                    .strongReference(true)
                    .register(globalRegistry);
        }

        @Override
        public TimeRecorder buildTimer() {
            Timer timer = Timer.builder(name)
                    .description(description)
                    .tags(tags)
                    .register(globalRegistry);

            return new MicrometerTimeRecorder(timer);
        }

        @Override
        public Runnable buildTimer(Runnable f) {
            Timer timer = Timer.builder(name)
                    .description(description)
                    .tags(tags)
                    .register(globalRegistry);

            return timer.wrap(f);
        }

        @Override
        public <T> Callable<T> buildTimer(Callable<T> f) {
            Timer timer = Timer.builder(name)
                    .description(description)
                    .tags(tags)
                    .register(globalRegistry);

            return timer.wrap(f);
        }

        @Override
        public <T> Supplier<T> buildTimer(Supplier<T> f) {
            Timer timer = Timer.builder(name)
                    .description(description)
                    .tags(tags)
                    .register(globalRegistry);

            return timer.wrap(f);
        }
    }

    static class MicrometerTimeRecorder implements TimeRecorder {
        Timer timer;

        MicrometerTimeRecorder(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void update(long amount, TimeUnit unit) {
            timer.record(amount, unit);
        }
    }
}
