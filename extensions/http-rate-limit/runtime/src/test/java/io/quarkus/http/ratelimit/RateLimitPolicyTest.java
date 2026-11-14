package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;

class RateLimitPolicyTest {

    private static RateLimitPolicy policy(long permits, Duration window, long burst) {
        return new DefaultRateLimitPolicy("api", RateLimitPhase.PRE_AUTHENTICATION, RateLimit.of(permits, window, burst), 1000);
    }

    @Test
    void derivedValues() {
        RateLimitPolicy p = policy(5, Duration.ofMinutes(1), 5);
        assertThat(p.emissionIntervalMicros()).isEqualTo(12_000_000L);
        assertThat(p.burstToleranceMicros()).isEqualTo(60_000_000L);
        assertThat(p.emissionIntervalNanos()).isEqualTo(12_000_000_000L);
        assertThat(p.burstToleranceNanos()).isEqualTo(60_000_000_000L);
        assertThat(p.refillTime()).isEqualTo(Duration.ofSeconds(60));
        assertThat(p.id()).isEqualTo("api/pre-authentication");
    }

    @Test
    void subMillisecondWindow() {
        // 1.5 µs, rounded up
        RateLimitPolicy p = policy(100, Duration.ofNanos(150_000), 100);
        assertThat(p.emissionIntervalMicros()).isEqualTo(2);
        assertThat(p.burstToleranceMicros()).isEqualTo(200);
    }

    @Test
    void rejectsInvalid() {
        assertThatThrownBy(() -> policy(0, Duration.ofMinutes(1), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permits");
        assertThatThrownBy(() -> policy(5, Duration.ofMinutes(1), 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("burst");
        assertThatThrownBy(() -> policy(100, Duration.ofNanos(99_000), 100))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 microsecond");
        assertThatThrownBy(() -> policy(1, Duration.ofDays(31), 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("30 days");
        assertThatThrownBy(() -> policy(1, Duration.ofDays(1), Long.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void phaseConfigNames() {
        assertThat(RateLimitPhase.PRE_AUTHENTICATION.configName()).isEqualTo("pre-authentication");
        assertThat(RateLimitPhase.POST_AUTHENTICATION.configName()).isEqualTo("post-authentication");
    }
}
