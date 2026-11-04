package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimitDecisionTest {
    @Test
    void remainingCannotBeNegative() {
        assertThatThrownBy(() -> RateLimitDecision.allowed(-1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remaining");
        assertThat(RateLimitDecision.allowed(0).remaining()).isZero();
    }

    @Test
    void retryAfterMustBePositiveAndRepresentableInNanoseconds() {
        for (Duration wait : new Duration[] { Duration.ZERO, Duration.ofSeconds(-1), Duration.ofSeconds(Long.MAX_VALUE) }) {
            assertThatThrownBy(() -> RateLimitDecision.rejected(wait)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("retryAfter");
        }
        assertThat(RateLimitDecision.rejected(Duration.ofNanos(1)).retryAfter()).isEqualTo(Duration.ofNanos(1));
    }
}
