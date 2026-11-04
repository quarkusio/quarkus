package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimitTest {
    @Test
    void burstDefaultsToPermits() {
        assertThat(RateLimit.of(100, Duration.ofMinutes(1)))
                .isEqualTo(new RateLimit(100, Duration.ofMinutes(1), 100));
        assertThat(RateLimit.of(100, Duration.ofMinutes(1), 10).burst()).isEqualTo(10);
    }

    @Test
    void invalidLimitsFailWhenCreated() {
        assertThatThrownBy(() -> RateLimit.of(0, Duration.ofMinutes(1))).hasMessageContaining("permits");
        assertThatThrownBy(() -> RateLimit.of(1, Duration.ZERO)).hasMessageContaining("window");
        assertThatThrownBy(() -> RateLimit.of(1, null)).hasMessageContaining("window");
        assertThatThrownBy(() -> RateLimit.of(1, Duration.ofMinutes(1), 0)).hasMessageContaining("burst");
        assertThatThrownBy(() -> RateLimit.of(2, Duration.ofNanos(1000))).hasMessageContaining("microsecond");
        assertThatThrownBy(() -> RateLimit.of(1, Duration.ofDays(31))).hasMessageContaining("30 days");
        assertThatThrownBy(() -> RateLimit.of(1, Duration.ofSeconds(Long.MAX_VALUE))).hasMessageContaining("overflow");
    }
}
