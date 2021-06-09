package io.quarkus.micrometer.runtime.binder.mpmetrics;

import org.eclipse.microprofile.metrics.MetricType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

public class CounterAdapter implements org.eclipse.microprofile.metrics.Counter, MeterHolder {

    Counter counter;

    public CounterAdapter register(MpMetadata metadata, MetricDescriptor descriptor, MeterRegistry registry) {
        if (counter == null || metadata.cleanDirtyMetadata()) {
            counter = io.micrometer.core.instrument.Counter.builder(descriptor.name())
                    .description(metadata.getDescription())
                    .baseUnit(metadata.getUnit())
                    .tags(descriptor.tags())
                    .register(registry);
        }

        return this;
    }

    @Override
    public void inc() {
        counter.increment();
    }

    @Override
    public void inc(long l) {
        counter.increment((double) l);
    }

    @Override
    public long getCount() {
        return (long) counter.count();
    }

    @Override
    public Meter getMeter() {
        return counter;
    }

    @Override
    public MetricType getType() {
        return MetricType.COUNTER;
    }
}
