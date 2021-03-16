package io.quarkus.micrometer.runtime.registry.json;

import java.util.Arrays;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.AbstractDistributionSummary;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.TimeWindowMax;

public class JsonDistributionSummary extends AbstractDistributionSummary {

    private final LongAdder count;
    private final DoubleAdder total;
    private final TimeWindowMax max;
    private final TimeWindowMin min;

    public JsonDistributionSummary(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig,
            double scale, boolean supportsAggregablePercentiles) {
        super(id, clock, distributionStatisticConfig, scale, supportsAggregablePercentiles);
        this.count = new LongAdder();
        this.total = new DoubleAdder();
        this.max = new TimeWindowMax(clock, distributionStatisticConfig);
        this.min = new TimeWindowMin(clock, distributionStatisticConfig);
    }

    @Override
    protected void recordNonNegative(double amount) {
        count.increment();
        total.add(amount);
        max.record(amount);
        min.record(amount);
    }

    @Override
    public long count() {
        return count.longValue();
    }

    @Override
    public double totalAmount() {
        return total.sum();
    }

    @Override
    public double max() {
        return max.poll();
    }

    public double min() {
        return min.poll();
    }

    @Override
    public Iterable<Measurement> measure() {
        return Arrays.asList(
                new Measurement(() -> (double) count(), Statistic.COUNT),
                new Measurement(this::totalAmount, Statistic.TOTAL),
                new Measurement(this::max, Statistic.MAX));
    }
}
