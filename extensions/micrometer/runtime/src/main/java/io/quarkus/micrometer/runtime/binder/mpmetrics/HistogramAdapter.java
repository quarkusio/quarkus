package io.quarkus.micrometer.runtime.binder.mpmetrics;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Snapshot;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

public class HistogramAdapter implements Histogram, MeterHolder {
    DistributionSummary summary;

    HistogramAdapter register(MpMetadata metadata, MetricDescriptor metricInfo, MeterRegistry registry) {
        if (summary == null || metadata.cleanDirtyMetadata()) {
            summary = io.micrometer.core.instrument.DistributionSummary.builder(metricInfo.name())
                    .description(metadata.getDescription())
                    .baseUnit(metadata.getUnit())
                    .tags(metricInfo.tags())
                    .register(registry);
        }

        return this;
    }

    @Override
    public void update(int i) {
        summary.record(i);
    }

    @Override
    public void update(long l) {
        summary.record(l);
    }

    @Override
    public long getCount() {
        return summary.count();
    }

    @Override
    public long getSum() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    /** Not supported. */
    @Override
    public Snapshot getSnapshot() {
        throw new UnsupportedOperationException("This operation is not supported when used with micrometer");
    }

    @Override
    public Meter getMeter() {
        return summary;
    }

    @Override
    public MetricType getType() {
        return MetricType.HISTOGRAM;
    }
}
