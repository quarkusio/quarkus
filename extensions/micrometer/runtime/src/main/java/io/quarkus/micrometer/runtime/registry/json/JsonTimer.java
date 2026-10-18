package io.quarkus.micrometer.runtime.registry.json;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.AbstractTimer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;

class JsonTimer extends AbstractTimer {

    private static final TimeUnit INTERNAL_STORAGE_UNIT = TimeUnit.NANOSECONDS;

    private final LongAdder elapsedTime; // in the internal storage unit
    private final LongAdder count;

    protected JsonTimer(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig,
            PauseDetector pauseDetector, TimeUnit baseTimeUnit) {
        super(id, clock, distributionStatisticConfig, pauseDetector, baseTimeUnit, false);
        this.count = new LongAdder();
        this.elapsedTime = new LongAdder();
    }

    @Override
    protected void recordNonNegative(long amount, TimeUnit unit) {
        count.increment();
        elapsedTime.add(INTERNAL_STORAGE_UNIT.convert(amount, unit));
    }

    @Override
    public long count() {
        return count.longValue();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        return unit.convert(elapsedTime.longValue(), INTERNAL_STORAGE_UNIT);
    }

    @Override
    public double max(TimeUnit unit) {
        return 0;
    }
}
