package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Tests how {@link OverloadDetector} adapts the concurrency limit, which is the part of
 * the TCP Vegas algorithm that decides what "overloaded" even means.
 * <p>
 * The adaptation estimates a queue length from how much the request time exceeds the lowest
 * request time seen recently, so every test first establishes that baseline with one sample
 * and then feeds request times relative to it. The estimate is
 * {@code ceil(limit * (1 - lowestRequestTime / lastRequestTime))}, and it is compared against
 * {@code alpha = alphaFactor * L} and {@code beta = betaFactor * L}, where {@code L}
 * is {@code 1 + log10(limit)}, {@code alphaFactor} defaults to 3 and {@code betaFactor} to 6.
 */
public class OverloadDetectorAdaptationTest {
    // makes the probe reset unreachable, so that tests which are not about the probe
    // are not disturbed by it raising the baseline at a random moment
    private static final double NO_PROBE = Double.MAX_VALUE;

    // the baseline request time, in microseconds; every other request time below is stated
    // as a multiple of it, which is all the estimate depends on
    private static final long BASELINE = 1_000;

    @Test
    public void firstSampleOnlyEstablishesTheBaseline() {
        OverloadDetector detector = detector(10);
        begin(detector, 10);

        // first request end triggers first adaptation, but there's no lowest request time yet,
        // so no change occurs
        detector.requestEnd(BASELINE);

        assertThat(detector.currentLimit()).isEqualTo(10);
    }

    @Test
    public void noQueueGrowsTheLimitALot() {
        // a request that is as fast as the fastest one seen means there's no queueing,
        // the limit grows by the largest step the algorithm has
        OverloadDetector detector = detector(10);
        begin(detector, 10);
        sample(detector, BASELINE);

        // limit is 10
        // `1 + log10(limit)` is 2
        // alpha is `3 * 2 = 6`
        // beta is `6 * 2 = 12`
        // `ceil(limit * (1 - 1000/1000))` is 0, which is below 2
        sample(detector, BASELINE);

        // grow by beta
        assertThat(detector.currentLimit()).isEqualTo(10 + 12);
    }

    @Test
    public void shortQueueGrowsTheLimitCautiously() {
        // a short queue (below alpha) means there is spare capacity,
        // though not enough to justify a large increase
        OverloadDetector detector = detector(10);
        begin(detector, 10);
        sample(detector, BASELINE);

        // limit is 10
        // `1 + log10(limit)` is 2
        // alpha is `3 * 2 = 6`
        // beta is `6 * 2 = 12`
        // `ceil(limit * (1 - 1000/1333))` is 3, which is above 2 and below alpha
        sample(detector, 1_333);

        // grow by `1 + log10(limit)`
        assertThat(detector.currentLimit()).isEqualTo(10 + 2);
    }

    @Test
    public void moderateQueueLeavesTheLimitAlone() {
        // a moderate queue (between alpha and beta) means the limit is OK
        // and doesn't need changing
        OverloadDetector detector = detector(10);
        begin(detector, 10);
        sample(detector, BASELINE);

        // limit is 10
        // `1 + log10(limit)` is 2
        // alpha is `3 * 2 = 6`
        // beta is `6 * 2 = 12`
        // `ceil(limit * (1 - 1000/2888))` is 7, which is above alpha and below beta
        sample(detector, 2_888);

        // no change
        assertThat(detector.currentLimit()).isEqualTo(10);
    }

    @Test
    public void longQueueShrinksTheLimit() {
        // A limit of 10 can never shrink, because the estimate is at most the limit itself
        // and beta is `6 * 2` at that limit. Shrinking only becomes reachable once the limit
        // is large enough for a queue to exceed beta.
        OverloadDetector detector = detector(100);
        begin(detector, 100);
        sample(detector, BASELINE);

        // limit is 100
        // `1 + log10(limit)` is 3
        // alpha is `3 * 3 = 6`
        // beta is `6 * 3 = 12`
        // `ceil(limit * (1 - 1000/2000))` is 50, which is above beta
        sample(detector, 2000);

        // shrink by `1 + log10(limit)`
        assertThat(detector.currentLimit()).isEqualTo(100 - 3);
    }

    @Test
    public void limitNeverExceedsTheMaximum() {
        OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(10)
                .withMaxLimit(15)
                .withProbeFactor(NO_PROBE)
                .build());
        begin(detector, 10);
        sample(detector, BASELINE);

        // would grow to 22, but max limit is 15
        sample(detector, BASELINE);

        assertThat(detector.currentLimit()).isEqualTo(15);
    }

    @Test
    public void limitIsNotAdaptedWhileConcurrencyIsLow() {
        // a request time measured while the application is nearly idle says nothing
        // about where the limit should sit, so it must not move it
        OverloadDetector detector = detector(10);
        begin(detector, 4); // `2 * 4` is below the limit of 10
        sample(detector, BASELINE);
        sample(detector, BASELINE);
        assertThat(detector.currentLimit()).isEqualTo(10);
    }

    @Test
    public void limitIsAdaptedOnceConcurrencyIsHighEnough() {
        // the counterpart of the test above: one more concurrent request
        // and the very same sample does move the limit
        OverloadDetector detector = detector(10);
        begin(detector, 5); // `2 * 5` reaches the limit of 10
        sample(detector, BASELINE);
        sample(detector, BASELINE);
        assertThat(detector.currentLimit()).isEqualTo(10 + 12);
    }

    @Test
    public void probeSuppressesAdaptation() {
        // A probe factor small enough that every sample is a probe. This is what a probe
        // factor of 0 would do, and the reason the configuration rejects it: the limit is
        // frozen at its initial value no matter what the application does
        OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(10)
                .withProbeFactor(1e-9)
                .build());
        begin(detector, 10);

        for (int i = 0; i < 50; i++) {
            sample(detector, BASELINE); // would otherwise grow the limit on every sample
        }

        assertThat(detector.currentLimit()).isEqualTo(10);
    }

    @Test
    public void probeRefreshesTheBaseline() {
        // the lowest request time only ever decreases, so without the probe an application
        // that gets faster (JIT warmup, cache filling up, noisy neighbor going away, etc.)
        // would keep being measured against a baseline from the past and would keep seeing
        // a queue that is no longer there
        //
        // the probe factor makes a probe happen within the first 10 samples at this limit
        // (the jitter puts the threshold between 0.5 and 1 times the limit) and leaves
        // at least 4 non-probe samples after each one
        OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(10)
                .withMaxLimit(1_000)
                .withProbeFactor(1.0)
                .build());
        begin(detector, 700); // keeps `2 * concurrency` above the limit throughout

        sample(detector, BASELINE);

        for (int i = 0; i < 200; i++) {
            // against the original baseline this is a queue of 6, which holds the limit
            // steady forever; once a probe makes it the new baseline, it is a queue of 0
            sample(detector, 2_222);
        }

        assertThat(detector.currentLimit())
                .describedAs("the limit only moves if a probe raised the baseline")
                .isGreaterThan(10);
    }

    @Test
    public void staleLowestRequestTimeForcesAProbe() throws InterruptedException {
        // the probe factor is set so high that the request-counted probe can never fire;
        // the only way the baseline can be refreshed is the ceiling on its staleness
        OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(10)
                .withMaxLimit(1_000)
                .withProbeFactor(NO_PROBE)
                .build(), TimeUnit.MILLISECONDS.toNanos(40));
        begin(detector, 10);
        sample(detector, BASELINE);

        Thread.sleep(400);

        sample(detector, 2_222); // fires the forced probe and becomes the new lowest
        sample(detector, 2_222); // matches the lowest, estimated queue length is 0, grow

        assertThat(detector.currentLimit()).isEqualTo(10 + 12);
    }

    private OverloadDetector detector(int initialLimit) {
        return new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(initialLimit)
                .withProbeFactor(NO_PROBE)
                .build());
    }

    private void begin(OverloadDetector detector, int count) {
        for (int i = 0; i < count; i++) {
            detector.requestBegin();
        }
    }

    // ends one request and immediately begins another, so that the number of concurrent
    // requests stays intact and the only change is the limit adaptation
    private void sample(OverloadDetector detector, long requestTime) {
        detector.requestEnd(requestTime);
        detector.requestBegin();
    }
}
