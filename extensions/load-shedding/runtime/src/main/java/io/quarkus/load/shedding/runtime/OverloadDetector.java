package io.quarkus.load.shedding.runtime;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
        maxLimit = config.maxLimit();
        alphaFactor = config.alphaFactor();
        betaFactor = config.betaFactor();
        probeFactor = config.probeFactor();
        currentLimit = config.initialLimit();
        resetProbeJitter();
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
