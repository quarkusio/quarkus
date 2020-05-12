package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.concurrent.atomic.LongAdder;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.MetricType;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

class ConcurrentGaugeImpl implements ConcurrentGauge, MeterHolder {
    final LongAdder longAdder = new LongAdder();
    Gauge gauge;

    ConcurrentGaugeImpl register(MpMetadata metadata, MetricDescriptor metricInfo, MeterRegistry registry) {
        gauge = io.micrometer.core.instrument.Gauge.builder(metricInfo.name(), longAdder, LongAdder::doubleValue)
                .description(metadata.getDescription())
                .baseUnit(metadata.getUnit())
                .tags(metricInfo.tags())
                .strongReference(true)
                .register(registry);
        return this;
    }

    @Override
    public long getCount() {
        return longAdder.longValue();
    }

    /**
     * Not supported for micrometer. Min/max values per dropwizard
     * would be provided by dropwizard capabilities if enabled.
     */
    @Override
    public long getMax() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    /**
     * Not supported for micrometer. Min/max values per dropwizard
     * would be provided by dropwizard capabilities if enabled.
     */
    @Override
    public long getMin() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public void inc() {
        longAdder.increment();
    }

    @Override
    public void dec() {
        longAdder.decrement();
    }

    @Override
    public Meter getMeter() {
        return gauge;
    }

    @Override
    public MetricType getType() {
        return MetricType.CONCURRENT_GAUGE;
    }
}
