package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Snapshot;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class TimerAdapter
        implements org.eclipse.microprofile.metrics.Timer, org.eclipse.microprofile.metrics.SimpleTimer, MeterHolder {
    final MeterRegistry registry;
    Timer timer;

    TimerAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    public TimerAdapter register(MpMetadata metadata, MetricDescriptor descriptor) {
        if (timer == null || metadata.cleanDirtyMetadata()) {
            timer = io.micrometer.core.instrument.Timer.builder(descriptor.name())
                    .description(metadata.getDescription())
                    .tags(descriptor.tags())
                    .register(registry);
        }
        return this;
    }

    public void update(long l, TimeUnit timeUnit) {
        timer.record(l, timeUnit);
    }

    @Override
    public void update(Duration duration) {
        timer.record(duration);
    }

    @Override
    public <T> T time(Callable<T> callable) throws Exception {
        return timer.wrap(callable).call();
    }

    @Override
    public void time(Runnable runnable) {
        timer.wrap(runnable);
    }

    @Override
    public SampleAdapter time() {
        return new SampleAdapter(timer, Timer.start(registry));
    }

    @Override
    public Duration getElapsedTime() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public long getCount() {
        return timer.count();
    }

    @Override
    public Duration getMaxTimeDuration() {
        return Duration.ofNanos(((long) timer.max(TimeUnit.NANOSECONDS)));
    }

    @Override
    public Duration getMinTimeDuration() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public double getFifteenMinuteRate() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public double getFiveMinuteRate() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public double getMeanRate() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public double getOneMinuteRate() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public Snapshot getSnapshot() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public Meter getMeter() {
        return timer;
    }

    public Timer.Sample start() {
        return Timer.start(registry);
    }

    public void stop(Timer.Sample sample) {
        sample.stop(timer);
    }

    class SampleAdapter implements org.eclipse.microprofile.metrics.Timer.Context, SimpleTimer.Context {
        final Timer timer;
        final Timer.Sample sample;

        SampleAdapter(Timer timer, Timer.Sample sample) {
            this.sample = sample;
            this.timer = timer;
        }

        @Override
        public long stop() {
            return sample.stop(timer);
        }

        @Override
        public void close() {
            sample.stop(timer);
        }
    }

    @Override
    public MetricType getType() {
        return MetricType.TIMER;
    }
}
