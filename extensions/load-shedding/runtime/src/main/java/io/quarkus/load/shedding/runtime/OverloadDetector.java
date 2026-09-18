package io.quarkus.load.shedding.runtime;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * An overload detector based on TCP Vegas, as implemented by
 * <a href="https://github.com/Netflix/concurrency-limits/">Netflix Concurrency Limits</a>.
 */
@Singleton
public class OverloadDetector {
    private static final int[] LOG10_PLUS_1_TABLE = new int[1_000];

    static {
        LOG10_PLUS_1_TABLE[0] = 1;
        for (int i = 1; i < 1_000; i++) {
            LOG10_PLUS_1_TABLE[i] = 1 + (int) Math.log10(i);
        }
    }

    static final double HYSTERESIS_FACTOR = 0.8;

    private final int maxLimit;
    private final int alphaFactor;
    private final int betaFactor;
    private final double probeFactor;

    private final AtomicInteger currentRequests = new AtomicInteger();
    private final AtomicLong lowestRequestTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong requestsSinceLastProbe = new AtomicLong();
    private volatile long currentLimit;
    private volatile boolean overloaded;

    private final ReentrantLock lock = new ReentrantLock();
    // this field is guarded by `lock`
    private double probeJitter;

    @Inject
    public OverloadDetector(LoadSheddingRuntimeConfig config) {
        validate(config);

        maxLimit = config.maxLimit();
        alphaFactor = config.alphaFactor();
        betaFactor = config.betaFactor();
        probeFactor = config.probeFactor();
        currentLimit = config.initialLimit();

        lock.lock();
        probeJitter = ThreadLocalRandom.current().nextDouble(0.5, 1);
        lock.unlock();
    }

    /**
     * The tuning knobs of the overload detection algorithm should typically be left untouched,
     * which is exactly why a mistake in one must not be swallowed silently. Every value rejected
     * here would otherwise produce load shedding that looks like it works and does not.
     */
    private static void validate(LoadSheddingRuntimeConfig config) {
        if (config.maxLimit() < 1) {
            throw new ConfigurationException("quarkus.load-shedding.max-limit must be at least 1, but is "
                    + config.maxLimit());
        }
        if (config.initialLimit() < 1) {
            throw new ConfigurationException("quarkus.load-shedding.initial-limit must be at least 1, but is "
                    + config.initialLimit());
        }
        // we could technically fail on `>=`, but it is a valid configuration (if a bit strange)
        // and makes a lot of tests easier to write
        if (config.initialLimit() > config.maxLimit()) {
            throw new ConfigurationException("quarkus.load-shedding.initial-limit must be less than or equal to"
                    + " max-limit, but is " + config.initialLimit() + " and max-limit is " + config.maxLimit());
        }
        if (config.alphaFactor() < 1) {
            throw new ConfigurationException("quarkus.load-shedding.alpha-factor must be at least 1, but is "
                    + config.alphaFactor());
        }
        if (config.betaFactor() <= config.alphaFactor()) {
            throw new ConfigurationException("quarkus.load-shedding.beta-factor must be greater than"
                    + " alpha-factor, but is " + config.betaFactor() + " and alpha-factor is " + config.alphaFactor());
        }
        if (!(config.probeFactor() > 0.0)) { // also rejects NaN
            throw new ConfigurationException("quarkus.load-shedding.probe-factor must be positive, but is "
                    + config.probeFactor());
        }
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public void requestBegin() {
        updateOverloaded(currentRequests.incrementAndGet());
    }

    public void requestEnd(long timeInMicros) {
        // we divide by the request time when the queue size is estimated below, so it must not be 0
        timeInMicros = Math.max(1, timeInMicros);

        int current = currentRequests.getAndDecrement();

        boolean isLowest = updateLowestRequestTime(timeInMicros);

        updateLimit(timeInMicros, isLowest, current);

        // after `updateLimit()`, so that the possibly adapted limit is used
        updateOverloaded(current - 1);
    }

    private boolean updateLowestRequestTime(long requestTime) {
        long current = lowestRequestTime.get();
        if (current > requestTime) {
            // a lost race means another thread set a lower value, or a probe reset raised
            // the baseline; either way this is not worth retrying
            lowestRequestTime.compareAndSet(current, requestTime);
            return true;
        }
        return false;
    }

    private void updateLimit(long requestTime, boolean isLowest, int currentRequests) {
        // must increment before the lock, so that requests skipped because of contention
        // still count towards the next probe
        requestsSinceLastProbe.incrementAndGet();

        if (!lock.tryLock()) {
            // someone else holds the lock and performs adaptation, we can skip that entirely
            return;
        }
        try {
            long probeCount = requestsSinceLastProbe.get(); // must read inside the lock
            if (probeFactor * probeJitter * currentLimit <= probeCount) {
                probeJitter = ThreadLocalRandom.current().nextDouble(0.5, 1);
                requestsSinceLastProbe.set(0);
                lowestRequestTime.set(requestTime);
                return;
            }

            // this request established a new baseline; the queue size computed below would be 0,
            // which would grow the limit without evidence that there is spare capacity
            if (isLowest) {
                return;
            }

            long currentLimit = this.currentLimit;

            if (2L * currentRequests < currentLimit) {
                return;
            }

            // the `lowestRequestTime` is updated outside the lock, in `updateLowestRequestTime()`
            // it is possible that between `updateLowestRequestTime()` and this point, another thread
            // have raised the `lowestRequestTime` past this request's time, and the queue size
            // below would then go negative, growing the limit as if there were no queue at all
            long lowest = lowestRequestTime.get();
            if (lowest > requestTime) {
                return;
            }

            int queueSize = (int) Math.ceil(currentLimit * (1.0 - (double) lowest / (double) requestTime));

            int currentLimitLog10Plus1;
            if (currentLimit >= 0 && currentLimit < 1_000) {
                currentLimitLog10Plus1 = LOG10_PLUS_1_TABLE[(int) currentLimit];
            } else {
                currentLimitLog10Plus1 = 1 + (int) Math.log10(currentLimit);
            }
            int alpha = alphaFactor * currentLimitLog10Plus1;
            int beta = betaFactor * currentLimitLog10Plus1;

            long newLimit;
            if (queueSize <= currentLimitLog10Plus1) {
                newLimit = currentLimit + beta;
            } else if (queueSize < alpha) {
                newLimit = currentLimit + currentLimitLog10Plus1;
            } else if (queueSize > beta) {
                newLimit = currentLimit - currentLimitLog10Plus1;
            } else {
                return;
            }

            newLimit = Math.clamp(newLimit, 1, maxLimit);
            this.currentLimit = newLimit;
        } finally {
            lock.unlock();
        }
    }

    // the overloaded state is hysteretic: it is entered when the number of concurrent
    // requests reaches the limit and it is only left when it drops well below it
    private void updateOverloaded(int currentRequests) {
        long limit = currentLimit;
        if (overloaded) {
            if (currentRequests < limit * HYSTERESIS_FACTOR) {
                overloaded = false;
            }
        } else {
            if (currentRequests >= limit) {
                overloaded = true;
            }
        }
    }
}
