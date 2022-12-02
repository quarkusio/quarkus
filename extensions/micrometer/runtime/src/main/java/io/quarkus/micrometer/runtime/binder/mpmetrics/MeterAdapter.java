package io.quarkus.micrometer.runtime.binder.mpmetrics;

import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class MeterAdapter implements Meter, MeterHolder {
    Counter counter;

    public MeterAdapter register(MpMetadata metadata, MetricDescriptor descriptor, MeterRegistry registry) {
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
    public void mark() {
        counter.increment();
    }

    @Override
    public void mark(long l) {
        counter.increment(l);
    }

    @Override
    public long getCount() {
        return (long) counter.count();
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
    public io.micrometer.core.instrument.Meter getMeter() {
        return counter;
    }

    @Override
    public MetricType getType() {
        return MetricType.METERED;
    }
}
