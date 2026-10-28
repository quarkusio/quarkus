package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class HttpLoadSheddingRetryAfterTest {
    // deliberately different from the production default
    private static final Duration MAX_RETRY_AFTER = Duration.ofMinutes(5);

    // enough samples that a range of several distinct values is practically certain
    // to show more than one of them
    private static final int SAMPLES = 1_000;

    @Test
    public void retryAfterFollowsTheOverloadDuration() {
        // the jitter spans 1 x the elapsed duration to 2 x the elapsed duration
        assertThat(retryAfter(Duration.ofSeconds(4))).isBetween(4L, 8L);
        assertThat(retryAfter(Duration.ofSeconds(20))).isBetween(20L, 40L);
    }

    @Test
    public void longerOverloadKeepsClientsAwayLonger() {
        // a brief spike must not black the application out for as long as a sustained overload
        long shorter = lowestRetryAfter(Duration.ofSeconds(5));
        long longer = lowestRetryAfter(Duration.ofSeconds(25));

        assertThat(shorter).isLessThan(longer);
    }

    // the jitter makes a single sample useless for comparisons, so compare the floors
    private long lowestRetryAfter(Duration overloadDuration) {
        long lowest = Long.MAX_VALUE;
        for (int i = 0; i < SAMPLES; i++) {
            lowest = Math.min(lowest, retryAfter(overloadDuration));
        }
        return lowest;
    }

    @Test
    public void retryAfterIsAtLeastOneSecond() {
        // `Retry-After` has a granularity of one second, so a freshly entered overload
        // would otherwise be sent as a zero and invite an immediate retry
        for (int i = 0; i < SAMPLES; i++) {
            assertThat(retryAfter(Duration.ZERO)).isBetween(1L, 2L);
            assertThat(retryAfter(Duration.ofMillis(100))).isBetween(1L, 2L);
            assertThat(retryAfter(Duration.ofMillis(900))).isBetween(1L, 2L);
        }
    }

    @Test
    public void retryAfterNeverExceedsTheMaximum() {
        // an overload that has lasted far longer than the maximum is still capped
        for (int i = 0; i < SAMPLES; i++) {
            assertThat(retryAfter(Duration.ofHours(3))).isBetween(1L, MAX_RETRY_AFTER.toSeconds());
        }
    }

    @Test
    public void retryAfterIsJitteredEvenAtTheMaximum() {
        // the longest overloads are exactly the ones that must not invite every client back
        // at the same moment, so the cap must not collapse the jitter into a single value
        Set<Long> values = new HashSet<>();
        for (int i = 0; i < SAMPLES; i++) {
            values.add(retryAfter(Duration.ofHours(3)));
        }

        assertThat(values).hasSizeGreaterThan(1);
    }

    @Test
    public void unreasonablyShortMaximumStillYieldsAValidHeader() {
        // a maximum shorter than the granularity of the header cannot be honored literally,
        // so the one second the header can express wins over a bound of zero
        for (int i = 0; i < SAMPLES; i++) {
            assertThat(retryAfter(Duration.ofNanos(5), Duration.ZERO)).isEqualTo(1L);
        }
    }

    @Test
    public void veryShortMaximumIsStillRespected() {
        // the jitter doubles the base, and the base has a floor of one second, so a maximum
        // that is itself around a second is the one case where the two can disagree
        for (int i = 0; i < SAMPLES; i++) {
            assertThat(retryAfter(Duration.ofHours(3), Duration.ofSeconds(1))).isEqualTo(1L);
            assertThat(retryAfter(Duration.ofHours(3), Duration.ofSeconds(2))).isBetween(1L, 2L);
            assertThat(retryAfter(Duration.ofHours(3), Duration.ofSeconds(3))).isBetween(1L, 3L);
        }
    }

    private long retryAfter(Duration overloadDuration) {
        return retryAfter(overloadDuration, MAX_RETRY_AFTER);
    }

    private long retryAfter(Duration overloadDuration, Duration maxRetryAfter) {
        return HttpLoadShedding.retryAfterSeconds(overloadDuration.toNanos(), maxRetryAfter);
    }
}
