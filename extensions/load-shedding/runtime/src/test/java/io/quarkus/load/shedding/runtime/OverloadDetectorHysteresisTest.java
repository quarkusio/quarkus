package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OverloadDetectorHysteresisTest {
    private static final int LIMIT = 10;
    private static final int CLEAR_AT = (int) (OverloadDetector.HYSTERESIS_FACTOR * LIMIT);

    // All assertions here are stated in absolute numbers of concurrent requests, which is
    // only meaningful when the limit stays intact. Two things keep it at `LIMIT`:
    //
    // - Request times are strictly decreasing, so every sample establishes a new lowest time
    // and `updateLimit()` always takes the new-lowest early return before it can adapt.
    // - `TestConfig` sets the probe factor to `Double.MAX_VALUE`, making the probe reset
    // unreachable. It would otherwise raise the lowest request time and break the invariant
    // above, and since jitter is random, it would do so unpredictably.
    //
    // Hysteresis is therefore tested in isolation from adaptation. Their interplay, notably
    // that `requestEnd()` evaluates the overloaded state against the limit that `updateLimit()`
    // has just chosen, is not covered here.
    private long nextRequestTime = 1_000_000;

    private final OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
            .withInitialLimit(LIMIT)
            .withProbeFactor(Double.MAX_VALUE)
            .build());

    @Test
    public void initiallyNotOverloaded() {
        assertThat(detector.isOverloaded()).isFalse();
    }

    @Test
    public void becomesOverloadedWhenLimitIsReached() {
        for (int i = 0; i < LIMIT - 1; i++) {
            begin(1);
            assertThat(detector.isOverloaded()).isFalse();
        }

        begin(1);
        assertThat(detector.isOverloaded()).isTrue();
    }

    @Test
    public void staysOverloadedUntilWellBelowLimit() {
        begin(LIMIT);
        assertThat(detector.isOverloaded()).isTrue();

        // down to 0.8 * limit, the overloaded state is retained
        for (int current = LIMIT - 1; current >= CLEAR_AT; current--) {
            end(1);
            assertThat(detector.isOverloaded()).isTrue();
        }

        end(1);
        assertThat(detector.isOverloaded()).isFalse();
    }

    @Test
    public void reentersOverloadedStateOnlyAtLimit() {
        begin(LIMIT);
        end(LIMIT - CLEAR_AT + 1); // now below 0.8 * limit
        assertThat(detector.isOverloaded()).isFalse();

        // back up to just below the limit
        begin(LIMIT - CLEAR_AT);
        assertThat(detector.isOverloaded()).isFalse();

        begin(1);
        assertThat(detector.isOverloaded()).isTrue();
    }

    @Test
    public void isOverloadedDoesNotChangeState() {
        begin(LIMIT - 1);
        for (int i = 0; i < 100; i++) {
            assertThat(detector.isOverloaded()).isFalse();
        }

        // the state only moves when the number of concurrent requests does

        begin(1);
        for (int i = 0; i < 100; i++) {
            assertThat(detector.isOverloaded()).isTrue();
        }
    }

    @Test
    public void overloadDurationIsZeroWhenNotOverloaded() {
        assertThat(detector.overloadDurationNanos()).isZero();

        begin(LIMIT - 1);
        assertThat(detector.overloadDurationNanos()).isZero();
    }

    @Test
    public void overloadDurationGrowsWhileOverloaded() throws InterruptedException {
        begin(LIMIT);

        Thread.sleep(10);
        long first = detector.overloadDurationNanos();
        assertThat(first).isPositive();

        Thread.sleep(10);
        long second = detector.overloadDurationNanos();
        assertThat(second).isGreaterThan(first);
    }

    @Test
    public void overloadDurationIsResetWhenOverloadEnds() throws InterruptedException {
        begin(LIMIT);
        Thread.sleep(100);
        long duration = detector.overloadDurationNanos();
        assertThat(duration).isPositive();

        // down to below 0.8 * limit, so the overloaded state is left
        end(LIMIT - CLEAR_AT + 1);
        assertThat(detector.isOverloaded()).isFalse();
        assertThat(detector.overloadDurationNanos()).isZero();

        // a new overload episode is timed from its own start, not from the previous one,
        // so its duration must be well below the 100 ms that the first episode lasted
        begin(LIMIT - CLEAR_AT + 1);
        assertThat(detector.isOverloaded()).isTrue();
        assertThat(detector.overloadDurationNanos()).isLessThan(duration);
    }

    private void begin(int count) {
        for (int i = 0; i < count; i++) {
            detector.requestBegin();
        }
    }

    private void end(int count) {
        for (int i = 0; i < count; i++) {
            detector.requestEnd(nextRequestTime--);
        }
    }
}
