package io.quarkus.load.shedding.runtime;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final int maxLimit;
    private final int alphaFactor;
    private final int betaFactor;
    private final double probeFactor;

    private final AtomicInteger currentRequests = new AtomicInteger();
    private volatile long currentLimit;

    private long lowestRequestTime = Long.MAX_VALUE;
    private double probeCount = 0.0;
    private double probeJitter;

    @Inject
    public OverloadDetector(LoadSheddingRuntimeConfig config) {
        validate(config);

        maxLimit = config.maxLimit();
        alphaFactor = config.alphaFactor();
        betaFactor = config.betaFactor();
        probeFactor = config.probeFactor();
        currentLimit = config.initialLimit();
        resetProbeJitter();
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
        return currentRequests.get() >= currentLimit;
    }

    public void requestBegin() {
        currentRequests.incrementAndGet();
    }

    public void requestEnd(long timeInMicros) {
        int current = currentRequests.getAndDecrement();

        update(timeInMicros, current);
    }

    private synchronized void update(long requestTime, int currentRequests) {
        probeCount++;
        if (probeFactor * probeJitter * currentLimit <= probeCount) {
            resetProbeJitter();
            probeCount = 0.0;
            lowestRequestTime = requestTime;
            return;
        }

        if (requestTime < lowestRequestTime) {
            lowestRequestTime = requestTime;
            return;
        }

        long currentLimit = this.currentLimit;

        if (2L * currentRequests < currentLimit) {
            return;
        }

        int queueSize = (int) Math.ceil(currentLimit * (1.0 - (double) lowestRequestTime / (double) requestTime));

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

        newLimit = Math.max(1, Math.min(maxLimit, newLimit));
        this.currentLimit = newLimit;
    }

    private void resetProbeJitter() {
        probeJitter = ThreadLocalRandom.current().nextDouble(0.5, 1);
    }
}
