package io.quarkus.http.ratelimit.runtime.internal.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitDecision.Allowed;
import io.quarkus.http.ratelimit.RateLimitDecision.Rejected;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.FailureMode;
import io.quarkus.http.ratelimit.runtime.internal.gcra.MutableClock;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.mutiny.Uni;

/**
 * A client over its limit must not cost a store call per request: the refusal is answered locally until its
 * {@code Retry-After} has passed, since no request of that key can be admitted before then.
 */
class RefusalCachingRateLimitStoreTest {

    // 1 permit per minute
    static final DefaultRateLimitPolicy POLICY = policy(1);

    final MutableClock clock = new MutableClock();
    final LocalRateLimitStore oracle = new LocalRateLimitStore(clock);
    final List<String> calls = new ArrayList<>();
    boolean failing;
    final RateLimitStore counting = new RateLimitStore() {
        @Override
        public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
            calls.add(key);
            if (failing) {
                return Uni.createFrom().failure(new IllegalStateException("down"));
            }
            return oracle.tryConsume(key, cost, policy);
        }
    };
    final LocalRateLimitStore local = new LocalRateLimitStore(clock);
    final GuardedRateLimitStore guarded = new GuardedRateLimitStore("s", new PassThroughGuard(),
            new AtomicReference<>(CircuitBreakerState.CLOSED), Duration.ofSeconds(10), new AtomicInteger(), 256,
            counting, Duration.ofMillis(100), FailureMode.LOCAL, local, clock);
    final RefusalCachingRateLimitStore store = new RefusalCachingRateLimitStore(guarded, POLICY, clock,
            builder -> {
            });

    static DefaultRateLimitPolicy policy(long permits) {
        return new DefaultRateLimitPolicy("r", RateLimitPhase.PRE_AUTHENTICATION, permits, Duration.ofMinutes(1),
                permits, 100);
    }

    RateLimitDecision consume(String key, RateLimitPolicy policy) {
        return store.tryConsume(key, 1, policy).await().indefinitely();
    }

    @Test
    void refusalIsAnsweredLocallyUntilRetryAfter() {
        assertThat(consume("a", POLICY)).isInstanceOf(Allowed.class);
        assertThat(consume("a", POLICY)).isInstanceOf(Rejected.class);
        assertThat(calls).hasSize(2);

        clock.advance(Duration.ofSeconds(20));
        RateLimitDecision cached = consume("a", POLICY);
        assertThat(cached).isInstanceOf(Rejected.class);
        assertThat(((Rejected) cached).retryAfter()).isEqualTo(Duration.ofSeconds(40));
        assertThat(calls).hasSize(2);

        clock.advance(Duration.ofSeconds(40));
        assertThat(consume("a", POLICY)).isInstanceOf(Allowed.class);
        assertThat(calls).hasSize(3);
    }

    @Test
    void otherKeysAndOtherPoliciesStillReachTheStore() {
        consume("a", POLICY);
        consume("a", POLICY);
        assertThat(consume("b", POLICY)).isInstanceOf(Allowed.class);
        // an override that gives the key another limit is answered by the store
        consume("a", policy(5));
        assertThat(calls).containsExactly("a", "a", "b", "a");
    }

    /**
     * A refusal of the local fallback reflects only this replica's bucket, not the store's: once the store is back, it
     * decides again.
     */
    @Test
    void fallbackRefusalsAreNotCached() {
        failing = true;
        assertThat(consume("a", POLICY)).isInstanceOf(Allowed.class);
        assertThat(consume("a", POLICY)).isInstanceOf(Rejected.class);
        failing = false;
        assertThat(consume("a", POLICY)).isInstanceOf(Allowed.class);
        assertThat(calls).hasSize(3);
    }

    @Test
    void costOtherThanOneIsNeverCached() {
        consume("a", POLICY);
        consume("a", POLICY);
        store.tryConsume("a", 0, POLICY).await().indefinitely();
        assertThat(calls).hasSize(3);
    }
}
