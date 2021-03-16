package io.quarkus.micrometer.runtime.registry.json;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.util.TimeUtils;

class TimeWindowMin {
    private static final AtomicIntegerFieldUpdater<TimeWindowMin> rotatingUpdater = AtomicIntegerFieldUpdater
            .newUpdater(TimeWindowMin.class, "rotating");

    private final Clock clock;
    private final long durationBetweenRotatesMillis;
    private AtomicLong[] ringBuffer;
    private int currentBucket;
    private volatile long lastRotateTimestampMillis;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private volatile int rotating = 0; // 0 - not rotating, 1 - rotating

    @SuppressWarnings("ConstantConditions")
    public TimeWindowMin(Clock clock, DistributionStatisticConfig config) {
        this(clock, config.getExpiry().toMillis(), config.getBufferLength());
    }

    public TimeWindowMin(Clock clock, long rotateFrequencyMillis, int bufferLength) {
        this.clock = clock;
        this.durationBetweenRotatesMillis = rotateFrequencyMillis;
        this.lastRotateTimestampMillis = clock.wallTime();
        this.currentBucket = 0;

        this.ringBuffer = new AtomicLong[bufferLength];
        for (int i = 0; i < bufferLength; i++) {
            this.ringBuffer[i] = new AtomicLong(Long.MAX_VALUE);
        }
    }

    /**
     * For use by timer implementations.
     *
     * @param sample The value to record.
     * @param timeUnit The unit of time of the incoming sample.
     */
    public void record(double sample, TimeUnit timeUnit) {
        record(() -> (long) TimeUtils.convert(sample, timeUnit, TimeUnit.NANOSECONDS));
    }

    private void record(LongSupplier sampleSupplier) {
        rotate();
        long sample = sampleSupplier.getAsLong();
        for (AtomicLong min : ringBuffer) {
            updateMin(min, sample);
        }
    }

    public double poll(TimeUnit timeUnit) {
        return poll(() -> TimeUtils.nanosToUnit(ringBuffer[currentBucket].get(), timeUnit));
    }

    private double poll(DoubleSupplier minSupplier) {
        rotate();
        synchronized (this) {
            return minSupplier.getAsDouble();
        }
    }

    public double poll() {
        return poll(() -> Double.longBitsToDouble(ringBuffer[currentBucket].get()));
    }

    public void record(double sample) {
        record(() -> Double.doubleToLongBits(sample));
    }

    private void updateMin(AtomicLong min, long sample) {
        long curMin;
        do {
            curMin = min.get();
        } while (curMin > sample && !min.compareAndSet(curMin, sample));
    }

    private void rotate() {
        long timeSinceLastRotateMillis = clock.wallTime() - lastRotateTimestampMillis;
        if (timeSinceLastRotateMillis < durationBetweenRotatesMillis) {
            // Need to wait more for next rotation.
            return;
        }

        if (!rotatingUpdater.compareAndSet(this, 0, 1)) {
            // Being rotated by other thread already.
            return;
        }

        try {
            int iterations = 0;
            synchronized (this) {
                do {
                    ringBuffer[currentBucket].set(0);
                    if (++currentBucket >= ringBuffer.length) {
                        currentBucket = 0;
                    }
                    timeSinceLastRotateMillis -= durationBetweenRotatesMillis;
                    lastRotateTimestampMillis += durationBetweenRotatesMillis;
                } while (timeSinceLastRotateMillis >= durationBetweenRotatesMillis && ++iterations < ringBuffer.length);
            }
        } finally {
            rotating = 0;
        }
    }

}
