package io.quarkus.http.ratelimit.runtime.internal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitPhase;

class DefaultRateLimitPolicyTest {

    /**
     * The emission interval is rounded up to whole microseconds, so that rounding never admits more than
     * {@code permits} per {@code window}: rounded down, 999 per millisecond would become 1 µs, that is 1000 per
     * millisecond.
     */
    @Test
    void emissionIntervalIsRoundedUp() {
        assertThat(policy(999, Duration.ofMillis(1)).emissionIntervalMicros()).isEqualTo(2);
        assertThat(policy(3, Duration.ofNanos(10_000)).emissionIntervalMicros()).isEqualTo(4);
        assertThat(policy(3, Duration.ofNanos(10_001)).emissionIntervalMicros()).isEqualTo(4);
        assertThat(policy(1000, Duration.ofMillis(1)).emissionIntervalMicros()).isEqualTo(1);
        assertThat(policy(100, Duration.ofMinutes(1)).emissionIntervalMicros()).isEqualTo(600_000);
    }

    private static DefaultRateLimitPolicy policy(long permits, Duration window) {
        return new DefaultRateLimitPolicy("r", RateLimitPhase.PRE_AUTHENTICATION, RateLimit.of(permits, window, 1), 10);
    }
}
