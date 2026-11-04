package io.quarkus.http.ratelimit.runtime.internal.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitLocalStoreCustomizer;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.gcra.MutableClock;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;

class LocalRateLimitStoreTest {

    static final Duration T = Duration.ofSeconds(12); // 5 permits per minute

    final MutableClock clock = new MutableClock();
    final LocalRateLimitStore store = new LocalRateLimitStore(clock);

    static RateLimitPolicy policy(long permits, Duration window, long burst, long maxKeys) {
        return new DefaultRateLimitPolicy("g", RateLimitPhase.PRE_AUTHENTICATION, permits, window, burst, maxKeys);
    }

    static RateLimitPolicy fivePerMinute() {
        return policy(5, Duration.ofMinutes(1), 5, 100);
    }

    RateLimitDecision consume(String key, long cost, RateLimitPolicy policy) {
        return store.tryConsume(key, cost, policy).await().indefinitely();
    }

    void exhaust(String key, RateLimitPolicy policy) {
        for (int i = 0; i < policy.burst(); i++) {
            assertThat(consume(key, 1, policy)).isInstanceOf(RateLimitDecision.Allowed.class);
        }
    }

    /**
     * What the {@code http.rate.limit.keys} gauge reads: expired keys are not counted, even with no traffic since.
     */
    long tracked(RateLimitPolicy policy) {
        return store.trackedKeys(policy);
    }

    @Test
    void admitsBurstThenRejectsAndRefills() {
        RateLimitPolicy policy = fivePerMinute();
        for (int i = 0; i < 5; i++) {
            assertThat(consume("k", 1, policy)).isEqualTo(RateLimitDecision.allowed(4 - i));
        }
        assertThat(consume("k", 1, policy)).isEqualTo(RateLimitDecision.rejected(T));

        clock.advance(T);

        assertThat(consume("k", 1, policy)).isEqualTo(RateLimitDecision.allowed(0));
    }

    @Test
    void keysAreIndependent() {
        RateLimitPolicy policy = fivePerMinute();
        exhaust("a", policy);
        assertThat(consume("a", 1, policy)).isInstanceOf(RateLimitDecision.Rejected.class);

        assertThat(consume("b", 1, policy)).isEqualTo(RateLimitDecision.allowed(4));
    }

    @Test
    void idleKeysExpireWhenTheirTatPasses() {
        RateLimitPolicy policy = fivePerMinute();
        consume("once", 1, policy); // TAT = now + T
        exhaust("five", policy); // TAT = now + 5T
        assertThat(tracked(policy)).isEqualTo(2);

        clock.advance(T.plusSeconds(1));
        assertThat(tracked(policy)).isEqualTo(1);
        assertThat(store.cache(policy).asMap()).containsOnlyKeys("five");

        clock.advance(T.multipliedBy(4).minusSeconds(2)); // 5T - 1s
        assertThat(store.cache(policy).asMap()).containsOnlyKeys("five");

        clock.advance(Duration.ofSeconds(2)); // 5T + 1s
        assertThat(tracked(policy)).isZero();
    }

    @Test
    void overriddenKeyExpiresOnItsOwnWindow() {
        RateLimitPolicy rule = fivePerMinute();
        RateLimitPolicy override = policy(5, Duration.ofHours(1), 5, 100);
        consume("a", 1, rule);
        consume("b", 1, override);
        assertThat(tracked(rule)).isEqualTo(2);
        assertThat(store.trackedKeys(override)).isEqualTo(2);

        clock.advance(Duration.ofMinutes(2));

        assertThat(tracked(rule)).isEqualTo(1);
        assertThat(store.cache(rule).asMap()).containsOnlyKeys("b");
    }

    @Test
    void sizeEvictionIsCountedAndLenient() {
        RateLimitPolicy policy = policy(5, Duration.ofMinutes(1), 5, 2);
        exhaust("a", policy);
        exhaust("b", policy);
        assertThat(consume("c", 1, policy)).isInstanceOf(RateLimitDecision.Allowed.class);

        assertThat(store.sizeEvictions(policy)).isGreaterThanOrEqualTo(1);
        assertThat(tracked(policy)).isLessThanOrEqualTo(2);
        List<RateLimitDecision> again = List.of(consume("a", 1, policy), consume("b", 1, policy));
        assertThat(again).contains(RateLimitDecision.allowed(4));
    }

    @Test
    void overrideChangesLimitMidStream() {
        exhaust("k", fivePerMinute());
        assertThat(consume("k", 1, fivePerMinute())).isInstanceOf(RateLimitDecision.Rejected.class);

        // GCRA debt is time: 5 x 12s = 60s is a full bucket under 10/1m burst 10 too (tau = 60s), one 6s permit away
        RateLimitPolicy tenPerMinute = policy(10, Duration.ofMinutes(1), 10, 100);
        assertThat(consume("k", 1, tenPerMinute))
                .isEqualTo(RateLimitDecision.rejected(Duration.ofSeconds(6)));

        // 10 per 2m with burst 10 has the same T and twice the tau: the same TAT leaves 5 permits, so 4 after this one
        RateLimitPolicy tenPerTwoMinutes = policy(10, Duration.ofMinutes(2), 10, 100);
        assertThat(consume("k", 1, tenPerTwoMinutes))
                .isEqualTo(RateLimitDecision.allowed(4));
    }

    @Test
    void costAboveBurstIsRejectedUpFront() {
        RateLimitPolicy policy = fivePerMinute();
        assertThatThrownBy(() -> store.tryConsume("k", 6, policy)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.tryConsume("k", -1, policy)).isInstanceOf(IllegalArgumentException.class);

        assertThat(store.trackedKeys(policy)).isZero();
        assertThat(consume("k", 5, policy)).isEqualTo(RateLimitDecision.allowed(0));
    }

    @Test
    void probeDoesNotConsume() {
        RateLimitPolicy policy = fivePerMinute();
        for (int i = 0; i < 10; i++) {
            assertThat(consume("k", 0, policy)).isEqualTo(RateLimitDecision.allowed(5));
        }
        assertThat(store.trackedKeys(policy)).isZero();
    }

    /**
     * A key whose override moves it from a 30 day policy to 5 per minute keeps a debt of up to 30 days. Capped at the
     * new burst tolerance, and stored so, it costs the key one emission interval, not 30 days.
     */
    @Test
    void debtFromALargerPolicyCostsAtMostOneEmissionInterval() {
        RateLimitPolicy monthly = policy(1, Duration.ofDays(30), 1, 100);
        assertThat(consume("k", 1, monthly)).isInstanceOf(RateLimitDecision.Allowed.class);

        assertThat(consume("k", 1, fivePerMinute())).isEqualTo(RateLimitDecision.rejected(T));
        clock.advance(T);
        assertThat(consume("k", 1, fivePerMinute())).isInstanceOf(RateLimitDecision.Allowed.class);
    }

    /**
     * The statistics of a customizer report the cache's evictions in the extension's terms: a key dropped to stay
     * within {@code max-keys}, which starts again with a full burst, and a key whose state ran out, which is the same as
     * no state.
     */
    @Test
    void customizerStatisticsReportEvictionsAndExpirations() {
        AtomicInteger evicted = new AtomicInteger();
        AtomicInteger expired = new AtomicInteger();
        RateLimitLocalStoreCustomizer customizer = new RateLimitLocalStoreCustomizer() {
            @Override
            public void customize(String rule, Settings settings) {
                settings.statistics(new Statistics() {
                    @Override
                    public void evicted() {
                        evicted.incrementAndGet();
                    }

                    @Override
                    public void expired() {
                        expired.incrementAndGet();
                    }
                });
            }
        };
        LocalRateLimitStore customized = new LocalRateLimitStore(clock, List.of(customizer),
                new ThrottledLoggers(clock, ThrottledLoggers.DEFAULT_INTERVAL));
        RateLimitPolicy oneKey = policy(5, Duration.ofMinutes(1), 5, 1);

        customized.tryConsume("a", 1, oneKey).await().indefinitely();
        customized.tryConsume("b", 1, oneKey).await().indefinitely();
        customized.cache(oneKey).cleanUp();
        assertThat(evicted).hasValue(1);

        clock.advance(T.plusSeconds(1));
        customized.cache(oneKey).cleanUp();
        assertThat(expired).hasValue(1);
    }

    /**
     * Two callers of the same key can take its lock in the opposite order from the times they read. A caller that
     * read the time early, and decides after another debited the key, must not rewrite the key's TAT earlier and
     * shorten that debit: with one permit a minute and a burst of 1, no two admissions are less than a minute apart.
     */
    @Test
    void lateDecisionDoesNotShortenAnotherDebit() throws InterruptedException {
        RateLimitPolicy oneAMinute = policy(1, Duration.ofMinutes(1), 1, 100);
        PausingClock pausing = new PausingClock(clock);
        LocalRateLimitStore shared = new LocalRateLimitStore(pausing);
        // built now, so that the first time A reads is the store's own
        shared.prepare((DefaultRateLimitPolicy) oneAMinute);
        Map<Long, RateLimitDecision> decisions = new ConcurrentHashMap<>();

        // A reads the time at 0 s and pauses there
        Thread a = new Thread(() -> decisions.put(0L, shared.tryConsume("k", 1, oneAMinute).await().indefinitely()));
        pausing.pause(a);
        a.start();
        assertThat(pausing.paused.await(5, TimeUnit.SECONDS)).isTrue();

        // B consumes at 30 s; where the time is read under the key's lock, B waits for A instead
        clock.advance(Duration.ofSeconds(30));
        Thread b = new Thread(() -> decisions.put(30L, shared.tryConsume("k", 1, oneAMinute).await().indefinitely()));
        b.start();
        b.join(500);
        pausing.resume.countDown();
        a.join(5_000);
        b.join(5_000);

        clock.advance(Duration.ofSeconds(30));
        decisions.put(60L, shared.tryConsume("k", 1, oneAMinute).await().indefinitely());

        List<Long> admitted = new ArrayList<>();
        for (long at : new long[] { 0, 30, 60 }) {
            if (decisions.get(at) instanceof RateLimitDecision.Allowed) {
                admitted.add(at);
            }
        }
        for (int i = 1; i < admitted.size(); i++) {
            assertThat(admitted.get(i) - admitted.get(i - 1)).as("admitted at %s", admitted).isGreaterThanOrEqualTo(60);
        }
    }

    /**
     * Pauses one thread right after its first reading of the time, until resumed.
     */
    static final class PausingClock implements RateLimitClock {

        final MutableClock delegate;
        final CountDownLatch paused = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        volatile Thread pausing;

        PausingClock(MutableClock delegate) {
            this.delegate = delegate;
        }

        void pause(Thread thread) {
            this.pausing = thread;
        }

        @Override
        public long nanoTime() {
            long now = delegate.nanoTime();
            if (Thread.currentThread() == pausing) {
                pausing = null;
                paused.countDown();
                try {
                    resume.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return now;
        }

        @Override
        public Cancellable schedule(Duration delay, Runnable task) {
            return delegate.schedule(delay, task);
        }
    }
}
