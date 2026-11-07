package io.quarkus.http.ratelimit.runtime.internal.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.FailureMode;
import io.quarkus.http.ratelimit.runtime.internal.gcra.MutableClock;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.mutiny.Uni;

/**
 * The recovery probe of a store runs only while the store is degraded: it is scheduled when the breaker opens, retried
 * while the breaker stays open, and no timer runs while the store is healthy.
 */
class StoreProbeTest {

    static final Duration DELAY = Duration.ofSeconds(5);
    static final RateLimitPolicy POLICY = new DefaultRateLimitPolicy("g", RateLimitPhase.PRE_AUTHENTICATION, 5,
            Duration.ofMinutes(1), 5, 100);

    final MutableClock clock = new MutableClock();
    final AtomicReference<CircuitBreakerState> breaker = new AtomicReference<>(CircuitBreakerState.CLOSED);
    final AtomicInteger probes = new AtomicInteger();

    StoreProbe probe() {
        RateLimitStore delegate = (key, cost, policy) -> {
            if (cost == 0) {
                probes.incrementAndGet();
            }
            return Uni.createFrom().item(RateLimitDecision.allowed(policy.burst()));
        };
        GuardedRateLimitStore guarded = new GuardedRateLimitStore("s", new PassThroughGuard(), breaker, DELAY,
                new AtomicInteger(), 256, delegate, Duration.ofMillis(100), FailureMode.LOCAL,
                new LocalRateLimitStore(clock), clock);
        return new StoreProbe(guarded, POLICY, clock, DELAY);
    }

    @Test
    void healthyStoreRunsNoTimer() {
        try (StoreProbe ignored = probe()) {
            assertThat(clock.scheduledCount()).isZero();
            clock.advance(DELAY.multipliedBy(10));
            assertThat(clock.scheduledCount()).isZero();
            assertThat(probes).hasValue(0);
        }
    }

    @Test
    void probesFromTheOpeningUntilTheStoreRecovers() {
        try (StoreProbe probe = probe()) {
            breaker.set(CircuitBreakerState.OPEN);
            probe.opened();
            clock.advance(DELAY.plusMillis(5));
            assertThat(probes).hasValue(1);

            // still degraded: retried one breaker delay later
            clock.advance(DELAY);
            assertThat(probes).hasValue(2);

            // recovered: the next tick neither probes nor schedules another
            breaker.set(CircuitBreakerState.CLOSED);
            long scheduled = clock.scheduledCount();
            clock.advance(DELAY);
            assertThat(probes).hasValue(2);
            clock.advance(DELAY.multipliedBy(10));
            assertThat(clock.scheduledCount()).isEqualTo(scheduled);
        }
    }
}
