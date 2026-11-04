package io.quarkus.http.ratelimit.runtime.internal.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitDecision.Allowed;
import io.quarkus.http.ratelimit.RateLimitDecision.Rejected;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.FailureMode;
import io.quarkus.http.ratelimit.runtime.internal.gcra.MutableClock;
import io.quarkus.http.ratelimit.runtime.internal.store.LeasingRateLimitStore.Overdraft;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * The lease decorator's state machine, clock-driven: decorators {@code A}, {@code B} (and {@code C}) over one
 * {@link LocalRateLimitStore} oracle, each behind a {@link ControllableStore} that passes calls through, holds a
 * reply, applies a call but drops its reply, delays it or fails it.
 * <p>
 * Unless a test says otherwise: 20 permits per minute, burst 20 ({@code T = 3 s}), lease-size 8 (lease time 24 s),
 * strict mode, failure-mode local, store timeout 100 ms.
 */
class LeasingRateLimitStoreTest {

    static final String KEY = "k";
    static final RateLimitPolicy POLICY = policy(20, 20);
    static final Duration T = Duration.ofSeconds(3);
    static final Duration TIMEOUT = Duration.ofMillis(100);

    final MutableClock clock = new MutableClock();
    final LocalRateLimitStore oracle = new LocalRateLimitStore(clock);

    static RateLimitPolicy policy(long permits, long burst) {
        return new DefaultRateLimitPolicy("g", RateLimitPhase.PRE_AUTHENTICATION,
                RateLimit.of(permits, Duration.ofMinutes(1), burst), 100);
    }

    // ---- fixture

    /**
     * The store behind a replica's guard: the oracle, with a behaviour the test switches at will. {@code HOLD},
     * {@code DROP_REPLY} and {@code DELAY} apply the call to the oracle at once (the store debited when the call
     * reached it) and only withhold the reply.
     */
    static final class ControllableStore implements RateLimitStore {

        enum Mode {
            PASS,
            HOLD,
            DROP_REPLY,
            DELAY,
            FAIL,
            NULL_RESULT
        }

        final LocalRateLimitStore oracle;
        final MutableClock clock;
        Mode mode = Mode.PASS;
        Duration delay = Duration.ZERO;
        final List<Long> costs = new ArrayList<>();
        final Deque<Runnable> held = new ArrayDeque<>();
        Runnable onCall = () -> {
        };
        int outstanding;
        int maxOutstanding;

        ControllableStore(LocalRateLimitStore oracle, MutableClock clock) {
            this.oracle = oracle;
            this.clock = clock;
        }

        @Override
        public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
            costs.add(cost);
            onCall.run();
            if (mode == Mode.FAIL) {
                return Uni.createFrom().failure(new IllegalStateException("down"));
            }
            if (mode == Mode.NULL_RESULT) {
                return Uni.createFrom().nullItem();
            }
            Uni<RateLimitDecision> applied = oracle.tryConsume(key, cost, policy);
            if (mode == Mode.PASS) {
                return applied;
            }
            RateLimitDecision decision = applied.await().indefinitely();
            return Uni.createFrom().emitter(em -> {
                outstanding++;
                maxOutstanding = Math.max(maxOutstanding, outstanding);
                Runnable reply = () -> {
                    outstanding--;
                    em.complete(decision);
                };
                switch (mode) {
                    case HOLD -> held.add(reply);
                    case DELAY -> clock.schedule(delay, reply);
                    default -> {
                    }
                }
            });
        }

        void release() {
            held.pollFirst().run();
        }

        long lastCost() {
            return costs.getLast();
        }
    }

    final class Replica {
        FailureMode failureMode = FailureMode.LOCAL;
        Duration timeout = TIMEOUT;
        long leaseSize = 8;
        long maxOverdraft;
        long maxKeys = 100;

        ControllableStore ctl;
        LocalRateLimitStore local;
        AtomicReference<CircuitBreakerState> breaker;
        GuardedRateLimitStore guard;
        LeasingRateLimitStore store;

        Replica failureMode(FailureMode mode) {
            this.failureMode = mode;
            return this;
        }

        Replica timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        Replica leaseSize(long leaseSize) {
            this.leaseSize = leaseSize;
            return this;
        }

        Replica maxOverdraft(long maxOverdraft) {
            this.maxOverdraft = maxOverdraft;
            return this;
        }

        Replica build() {
            ctl = new ControllableStore(oracle, clock);
            local = new LocalRateLimitStore(clock);
            breaker = new AtomicReference<>(CircuitBreakerState.CLOSED);
            guard = new GuardedRateLimitStore("s", new PassThroughGuard(), breaker, Duration.ofSeconds(10),
                    new AtomicInteger(), 256, ctl, timeout, failureMode, local, clock);
            store = new LeasingRateLimitStore(guard, leaseSize, maxOverdraft, maxKeys, timeout, clock, builder -> {
            });
            return this;
        }

        UniAssertSubscriber<RateLimitDecision> request(String key, RateLimitPolicy policy) {
            return store.tryConsume(key, 1, policy).subscribe().withSubscriber(UniAssertSubscriber.create());
        }

        UniAssertSubscriber<RateLimitDecision> request() {
            return request(KEY, POLICY);
        }

        List<UniAssertSubscriber<RateLimitDecision>> requests(int n, RateLimitPolicy policy) {
            List<UniAssertSubscriber<RateLimitDecision>> subs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                subs.add(request(KEY, policy));
            }
            return subs;
        }

        /**
         * One batch per size: the requests arrive while the claim is held, so the chunk is fully used in one step and
         * the target doubles without a prefetch.
         */
        void ramp(RateLimitPolicy policy, int... sizes) {
            ControllableStore.Mode before = ctl.mode;
            for (int size : sizes) {
                ctl.mode = ControllableStore.Mode.HOLD;
                List<UniAssertSubscriber<RateLimitDecision>> subs = requests(size, policy);
                ctl.release();
                subs.forEach(LeasingRateLimitStoreTest::assertAllowed);
                assertThat(ctl.lastCost()).isEqualTo(size);
            }
            ctl.mode = before;
        }

        long localRemaining(RateLimitPolicy policy) {
            return remaining(local, policy);
        }

        long ledger() {
            return store.overdraft(Overdraft.SETTLED) + store.overdraft(Overdraft.SETTLED_FALLBACK)
                    + store.overdraft(Overdraft.SETTLED_LATE) + store.overdraft(Overdraft.FORGIVEN)
                    + store.unreconciledOutstanding() + store.unreserved(KEY) + store.reserved(KEY);
        }

        void assertLedgerReconciles() {
            for (Overdraft o : Overdraft.values()) {
                assertThat(store.overdraft(o)).as(o.name()).isGreaterThanOrEqualTo(0);
            }
            assertThat(ledger()).as("ledger").isEqualTo(store.overdraft(Overdraft.ADMITTED));
        }

        /**
         * Forces a size eviction of every entry, the way {@code maxKeys} would; W-TinyLFU's choice of victim is not
         * deterministic for a handful of keys, so the eviction path is exercised by dropping the maximum to 0.
         */
        void evictAll() {
            store.cache().policy().eviction().orElseThrow().setMaximum(0);
            store.cache().policy().eviction().orElseThrow().setMaximum(maxKeys);
            // the effects of an eviction run after the cache call that caused it; here that call is the next one
            store.trackedKeys();
        }
    }

    Replica replica() {
        return new Replica().build();
    }

    static long remaining(LocalRateLimitStore store, RateLimitPolicy policy) {
        // the probe is refused by an empty bucket
        return store.tryConsume(KEY, 0, policy).await().indefinitely() instanceof Allowed allowed
                ? allowed.remaining()
                : 0;
    }

    long oracleRemaining() {
        return remaining(oracle, POLICY);
    }

    /**
     * Debits the oracle down to {@code left} permits.
     */
    void drainOracleTo(RateLimitPolicy policy, long left) {
        long take = remaining(oracle, policy) - left;
        if (take > 0) {
            assertThat(oracle.tryConsume(KEY, take, policy).await().indefinitely()).isInstanceOf(Allowed.class);
        }
    }

    static void assertAllowed(UniAssertSubscriber<RateLimitDecision> sub) {
        assertThat(sub.getItem()).isInstanceOf(Allowed.class);
    }

    static Rejected assertRejected(UniAssertSubscriber<RateLimitDecision> sub) {
        assertThat(sub.getItem()).isInstanceOf(Rejected.class);
        return (Rejected) sub.getItem();
    }

    static void assertUnavailable(UniAssertSubscriber<RateLimitDecision> sub) {
        assertThat(sub.getFailure()).isInstanceOf(StoreUnavailableException.class);
    }

    static void assertPending(UniAssertSubscriber<RateLimitDecision> sub) {
        sub.assertNotTerminated();
    }

    static long count(List<UniAssertSubscriber<RateLimitDecision>> subs, Class<? extends RateLimitDecision> type) {
        return subs.stream().filter(s -> type.isInstance(s.getItem())).count();
    }

    // ---- sizing without knowing the replica count

    @Test
    void coldReplicaStartsAtOneAndDoubles() {
        RateLimitPolicy wide = policy(100, 100);
        Replica a = replica();
        a.ramp(wide, 1, 2, 4, 8, 8);
        assertThat(a.ctl.costs).containsExactly(1L, 2L, 4L, 8L, 8L);
        assertThat(remaining(oracle, wide)).isEqualTo(100 - 23);
    }

    @Test
    void tailCapLimitsGrowthNearEmpty() {
        RateLimitPolicy small = policy(8, 8);
        Replica a = replica();
        a.ramp(small, 1, 2);
        assertThat(remaining(oracle, small)).isEqualTo(5);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(4, small);
        // remaining 5 → min(target 4, 5 / 2) = 2, not 4
        assertThat(a.ctl.lastCost()).isEqualTo(2);
        a.ctl.release();
        assertThat(count(subs, Allowed.class)).isEqualTo(2);
        // the waiters left are claimed for with the tail cap halving again: 3 / 2 = 1
        assertThat(a.ctl.lastCost()).isEqualTo(1);
    }

    @Test
    void idleReplicaShrinksAndHoldsNothing() {
        // T = 0.6 s, lease time 4.8 s; the tail cap never binds
        RateLimitPolicy wide = policy(100, 100);
        Replica a = replica();
        a.ramp(wide, 1, 2, 4);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> two = a.requests(2, wide);
        assertThat(a.ctl.lastCost()).isEqualTo(8);
        a.ctl.release();
        two.forEach(LeasingRateLimitStoreTest::assertAllowed);
        assertThat(a.store.held(KEY)).isEqualTo(6);
        a.ctl.mode = ControllableStore.Mode.PASS;

        clock.advance(Duration.ofSeconds(5));
        assertAllowed(a.request(KEY, wide));
        assertThat(a.store.stranded()).isEqualTo(6);
        // served 2 of 8: the next claim is sized by what was used
        assertThat(a.ctl.lastCost()).isEqualTo(2);
        assertThat(a.store.held(KEY)).isEqualTo(1);

        // the entry outlives its last chunk by one lease time, then nothing is held at all
        clock.advance(Duration.ofSeconds(5));
        assertThat(a.store.trackedKeys()).isEqualTo(1);
        clock.advance(Duration.ofSeconds(5));
        assertThat(a.store.trackedKeys()).isZero();
        assertThat(a.store.held(KEY)).isZero();
        assertThat(a.store.stranded()).isEqualTo(7);
    }

    @Test
    void tailIsNotGrabbedWhole() {
        RateLimitPolicy p = policy(30, 30);
        Replica a = replica();
        Replica b = replica();
        a.ramp(p, 1, 2, 4);
        // others take 9 meanwhile; A's stale estimate (23) lets it claim 8, and the grant reports 6 left
        drainOracleTo(p, 14);
        a.ramp(p, 8);
        assertThat(remaining(oracle, p)).isEqualTo(6);
        // A's target is 8 and its estimate 6: it claims min(8, 6 / 2) = 3
        assertAllowed(a.request(KEY, p));
        assertThat(a.ctl.lastCost()).isEqualTo(3);
        assertThat(remaining(oracle, p)).isEqualTo(3);
        // B starts at 1 and, with estimate 2 and target 2, claims min(2, 2 / 2) = 1
        assertAllowed(b.request(KEY, p));
        assertAllowed(b.request(KEY, p));
        assertThat(b.ctl.costs).containsExactly(1L, 1L);
    }

    @Test
    void scaleOutDoesNotIncreaseStranding() {
        RateLimitPolicy wide = policy(100, 100);
        Replica a = replica();
        Replica b = replica();
        Replica c = replica();
        a.ramp(wide, 1, 2, 4, 8);
        b.ramp(wide, 1, 2, 4, 8);
        c.ramp(wide, 1, 2);
        c.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> two = c.requests(2, wide);
        assertThat(c.ctl.lastCost()).isEqualTo(4);
        c.ctl.release();
        two.forEach(LeasingRateLimitStoreTest::assertAllowed);
        long held = a.store.held(KEY) + b.store.held(KEY) + c.store.held(KEY);
        // nothing beyond what the replicas' own recent demand sized
        assertThat(held).isEqualTo(2).isLessThanOrEqualTo(8 + 8 + 4);
        assertThat(a.store.stranded() + b.store.stranded() + c.store.stranded()).isZero();

        c.store.close();
        assertThat(c.store.stranded()).isEqualTo(2);
        assertThat(c.store.trackedKeys()).isZero();
        c.store.close();
        assertThat(c.store.stranded()).isEqualTo(2);
    }

    // ---- authoritative permits: at most once, within leaseTime of send

    @Test
    void chunkDeadlineIsAnchoredAtSendTime() {
        Replica a = new Replica().timeout(Duration.ofSeconds(60)).build();
        a.ramp(POLICY, 1, 2);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        UniAssertSubscriber<RateLimitDecision> first = a.request();
        assertThat(a.ctl.lastCost()).isEqualTo(4);
        clock.advance(Duration.ofSeconds(20));
        a.ctl.release();
        assertAllowed(first);
        // sent at 0, deadline 24: usable for 4 s only
        a.ctl.mode = ControllableStore.Mode.PASS;
        clock.advance(Duration.ofSeconds(3));
        assertAllowed(a.request());
        assertThat(a.ctl.costs).hasSize(3);
        clock.advance(Duration.ofSeconds(2));
        assertAllowed(a.request());
        assertThat(a.store.stranded()).isEqualTo(2);
        assertThat(a.ctl.costs).hasSize(4);

        // a reply delayed past its own deadline is stranded whole and never served
        assertAllowed(a.request());
        assertThat(a.store.held(KEY)).isZero();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        long cost = a.ctl.lastCost();
        clock.advance(Duration.ofSeconds(25));
        a.ctl.release();
        assertThat(a.store.stranded()).isEqualTo(2 + cost);
        assertThat(a.store.held(KEY)).isZero();
        assertPending(waiter);
        // the waiter is claimed for again
        assertThat(a.ctl.held).hasSize(1);
        a.ctl.release();
        assertAllowed(waiter);
    }

    @Test
    void prefetchNeverExtendsOldPermits() {
        RateLimitPolicy wide = policy(100, 100);
        Replica a = replica();
        a.ramp(wide, 1, 2, 4);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> five = a.requests(5, wide);
        assertThat(a.ctl.lastCost()).isEqualTo(8);
        a.ctl.release();
        five.forEach(LeasingRateLimitStoreTest::assertAllowed);
        a.ctl.mode = ControllableStore.Mode.PASS;
        clock.advance(Duration.ofSeconds(1));
        // down to 2 of 8: the prefetch lands chunk2 with a later deadline
        assertAllowed(a.request(KEY, wide));
        assertThat(a.ctl.lastCost()).isEqualTo(8);
        assertThat(a.store.held(KEY)).isEqualTo(10);

        clock.advance(Duration.ofMillis(4800 - 1000));
        // chunk1's deadline (8 × 0.6 s = 4.8 s after its send) has passed: its 2 are stranded, chunk2 untouched
        assertAllowed(a.request(KEY, wide));
        assertThat(a.store.stranded()).isEqualTo(2);
        assertThat(a.store.held(KEY)).isEqualTo(7);
    }

    @Test
    void envelopeOverArbitraryIntervals() {
        Random random = new Random(42);
        Replica a = new Replica().failureMode(FailureMode.CLOSED).timeout(Duration.ofSeconds(30)).build();
        Replica b = new Replica().failureMode(FailureMode.CLOSED).timeout(Duration.ofSeconds(30)).build();
        int seconds = 120;
        long[] served = new long[seconds + 31];
        for (Replica r : List.of(a, b)) {
            r.ctl.mode = ControllableStore.Mode.DELAY;
        }
        for (int t = 0; t < seconds; t++) {
            for (Replica r : List.of(a, b)) {
                r.ctl.delay = Duration.ofMillis(random.nextInt(10_001));
                // as fast as chunks allow: stop at the first request that is not admitted at once
                for (int i = 0; i < 20; i++) {
                    UniAssertSubscriber<RateLimitDecision> sub = r.store.tryConsume(KEY, 1, POLICY)
                            .onItem().invoke(d -> {
                                if (d instanceof Allowed) {
                                    served[(int) (clock.nanoTime() / 1_000_000_000L)]++;
                                }
                            })
                            .subscribe().withSubscriber(UniAssertSubscriber.create());
                    if (!(sub.getItem() instanceof Allowed)) {
                        break;
                    }
                }
            }
            clock.advance(Duration.ofSeconds(1));
        }
        clock.advance(Duration.ofSeconds(30));
        double rate = 20.0 / 60;
        for (int w : new int[] { 3, 24, 60 }) {
            double bound = 20 + rate * (w + 24);
            for (int t = 0; t + w <= served.length; t++) {
                long sum = 0;
                for (int i = t; i < t + w; i++) {
                    sum += served[i];
                }
                assertThat(sum).as("window [%d, %d)", t, t + w).isLessThanOrEqualTo((long) Math.floor(bound));
            }
        }
        assertThat(a.store.claims(true) + b.store.claims(true)).isPositive();
    }

    @Test
    void lostReplyNeverServesTwice() {
        Replica a = new Replica().failureMode(FailureMode.CLOSED).build();
        a.ramp(POLICY, 1, 2);
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        assertThat(a.ctl.lastCost()).isEqualTo(4);
        assertThat(oracleRemaining()).isEqualTo(20 - 3 - 4);
        clock.advance(TIMEOUT);
        assertUnavailable(waiter);
        assertThat(a.store.held(KEY)).isZero();
        // the store debited 4 that this replica will never serve
        assertThat(oracleRemaining()).isEqualTo(13);
    }

    // ---- refusal vs held permits

    @Test
    void refusedPrefetchDoesNotBlockHeldPermits() {
        RateLimitPolicy p = policy(12, 12);
        Replica a = replica();
        Replica b = replica();
        a.ramp(p, 1, 2);
        assertAllowed(a.request(KEY, p));
        assertThat(a.ctl.lastCost()).isEqualTo(4);
        assertAllowed(a.request(KEY, p));
        assertThat(a.store.held(KEY)).isEqualTo(2);
        // another replica drains the bucket meanwhile
        for (int i = 0; i < 5; i++) {
            assertAllowed(b.request(KEY, p));
        }
        assertThat(remaining(oracle, p)).isZero();

        // the prefetch at 1 of 4 left is refused: no rejection, no event, held permits still served
        assertAllowed(a.request(KEY, p));
        assertThat(a.ctl.costs).hasSize(4);
        assertThat(a.store.claims(false)).isEqualTo(1);
        assertAllowed(a.request(KEY, p));
        assertThat(a.store.held(KEY)).isZero();
        // near an empty bucket: the claim of 2 and then the single claim are both refused, two calls
        Rejected rejected = assertRejected(a.request(KEY, p));
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofSeconds(5));
        assertThat(a.ctl.costs).hasSize(6);
    }

    @Test
    void singlePermitFallbackCreditsExactlyOne() {
        RateLimitPolicy p = policy(30, 30);
        Replica a = replica();
        Replica b = replica();
        a.ramp(p, 1, 2, 4);
        b.ramp(p, 1, 2, 4, 8);
        assertAllowed(b.request(KEY, p));
        assertThat(b.ctl.lastCost()).isEqualTo(4);
        for (int i = 0; i < 3; i++) {
            assertAllowed(b.request(KEY, p));
        }
        assertAllowed(b.request(KEY, p));
        assertThat(b.ctl.lastCost()).isEqualTo(2);
        assertAllowed(b.request(KEY, p));
        assertThat(remaining(oracle, p)).isEqualTo(2);

        a.ctl.costs.clear();
        assertAllowed(a.request(KEY, p));
        assertThat(a.ctl.costs).containsExactly(8L, 1L);
        // the single grant reported 1 left: the tail cap sizes the next claim at 1 straight away
        assertAllowed(a.request(KEY, p));
        assertThat(a.ctl.costs).containsExactly(8L, 1L, 1L);
        Rejected rejected = assertRejected(a.request(KEY, p));
        assertThat(a.ctl.costs).containsExactly(8L, 1L, 1L, 1L);
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofSeconds(2));
        assertThat(remaining(oracle, p)).isZero();
        // cached: no store call
        assertRejected(a.request(KEY, p));
        assertThat(a.ctl.costs).hasSize(4);
    }

    @Test
    void cachedRefusalIsAFloorNotAPromise() {
        Replica a = replica();
        Replica b = replica();
        drainOracleTo(POLICY, 0);
        Rejected first = assertRejected(a.request());
        assertThat(first.retryAfter()).isEqualTo(T);
        clock.advance(T);
        // another replica takes the regenerated permit first
        assertAllowed(b.request());
        Rejected again = assertRejected(a.request());
        assertThat(again.retryAfter()).isEqualTo(T);
        assertThat(a.ctl.costs).hasSize(2);
        assertRejected(a.request());
        assertThat(a.ctl.costs).hasSize(2);
    }

    // ---- waiting

    @Test
    void waiterDeadlineBoundsTheStoreStage() {
        Replica a = new Replica().leaseSize(1).failureMode(FailureMode.OPEN).build();
        a.ctl.mode = ControllableStore.Mode.DELAY;
        a.ctl.delay = Duration.ofMillis(90);
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(50, POLICY);
        clock.advance(Duration.ofMillis(89));
        assertThat(count(subs, Allowed.class)).isZero();
        clock.advance(Duration.ofMillis(1));
        assertThat(count(subs, Allowed.class)).isEqualTo(1);
        clock.advance(Duration.ofMillis(10));
        // 100 ms after arrival every waiter has an answer: the failure-mode decision for the uncovered ones
        assertThat(count(subs, Allowed.class)).isEqualTo(50);
        // the chained claim continues and its chunk is kept
        clock.advance(Duration.ofMillis(80));
        assertThat(a.store.held(KEY)).isEqualTo(1);
        assertThat(a.ctl.costs).containsExactly(1L, 1L);
    }

    @Test
    void cancelledWaiterLeavesTheClaimRunning() {
        Replica a = replica();
        a.ramp(POLICY, 1, 2);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(3, POLICY);
        assertThat(a.ctl.lastCost()).isEqualTo(4);
        subs.get(1).cancel();
        a.ctl.release();
        assertAllowed(subs.get(0));
        assertAllowed(subs.get(2));
        assertThat(a.store.held(KEY)).isEqualTo(2);
    }

    @Test
    void waiterIsAnsweredAtItsDeadlineWithoutOtherEvents() {
        Replica a = new Replica().leaseSize(1).build();
        a.ctl.mode = ControllableStore.Mode.DELAY;
        a.ctl.delay = Duration.ofMillis(90);
        UniAssertSubscriber<RateLimitDecision> w1 = a.request();
        UniAssertSubscriber<RateLimitDecision> w2 = a.request();
        clock.advance(Duration.ofMillis(90));
        assertAllowed(w1);
        assertPending(w2);
        assertThat(a.ctl.costs).hasSize(2);
        clock.advance(Duration.ofMillis(10));
        // answered by the timer at 100 ms from the local bucket, not by claim #2 at 180 ms
        assertAllowed(w2);
        assertThat(a.localRemaining(POLICY)).isEqualTo(19);
        clock.advance(Duration.ofMillis(80));
        assertThat(a.store.held(KEY)).isEqualTo(1);
    }

    @Test
    void pendingClaimKeepsTheEntryAlive() {
        Replica a = new Replica().timeout(Duration.ofSeconds(40)).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        clock.advance(Duration.ofSeconds(30));
        assertThat(a.store.trackedKeys()).isEqualTo(1);
        a.ctl.release();
        // granted after its 24 s deadline: stranded, the waiter is claimed for again
        assertThat(a.store.stranded()).isEqualTo(1);
        assertPending(waiter);
        clock.advance(Duration.ofSeconds(10));
        // the waiter's deadline (40 s) is before claim #2's own (70 s): the timer answers it
        assertAllowed(waiter);
        assertThat(a.localRemaining(POLICY)).isEqualTo(19);
        assertThat(a.store.trackedKeys()).isEqualTo(1);
        a.ctl.release();
        assertThat(a.store.held(KEY)).isEqualTo(1);
        // chunk deadline 54 s, retained one lease time beyond it
        clock.advance(Duration.ofSeconds(40));
        assertThat(a.store.trackedKeys()).isZero();

        // a cancelled last waiter lets the entry expire too
        UniAssertSubscriber<RateLimitDecision> cancelled = a.request();
        cancelled.cancel();
        clock.advance(Duration.ofSeconds(100));
        assertThat(a.store.trackedKeys()).isEqualTo(1);
        a.ctl.release();
        clock.advance(Duration.ofSeconds(25));
        assertThat(a.store.trackedKeys()).isZero();
    }

    /**
     * Requests past the waiter cap are answered by the local bucket, whatever the failure mode: the store is healthy,
     * so {@code open} must not admit them all, nor {@code closed} refuse them all.
     */
    @ParameterizedTest
    @MethodSource("failureModes")
    void waiterCapAnswersWithTheLocalBucket(FailureMode mode) {
        Replica a = new Replica().failureMode(mode).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(1_000 + 21, POLICY);
        for (int i = 0; i < 1_000; i++) {
            assertPending(subs.get(i));
        }
        List<UniAssertSubscriber<RateLimitDecision>> overflow = subs.subList(1_000, subs.size());
        assertThat(count(overflow, Allowed.class)).isEqualTo(20);
        assertRejected(overflow.getLast());
        assertThat(a.ctl.costs).hasSize(1);
    }

    /**
     * Waiters queue in deadline order, so an arrival that does not change which deadline is due first leaves the entry
     * timer alone: a stall on a hot key must not cost a timer, and a walk over every waiter, per request.
     */
    @Test
    void arrivalsDuringAHeldClaimKeepTheEntryTimer() {
        Replica a = replica();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        long before = clock.scheduledCount();
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(1_000, POLICY);
        subs.forEach(LeasingRateLimitStoreTest::assertPending);
        assertThat(clock.scheduledCount() - before).isLessThan(10);
        a.ctl.release();
        // the claim of 1 serves the first waiter; the others wait for the next claims or their deadline
        assertAllowed(subs.getFirst());
    }

    /**
     * Held permits depend only on the emission interval and the burst: a policy that keeps both, such as 40 per 2
     * minutes against 20 per minute, keeps the entry, so that an override alternating between such limits does not
     * strand the key's chunks on every request.
     */
    @Test
    void policyWithTheSameIntervalAndBurstKeepsTheEntry() {
        Replica a = replica();
        RateLimitPolicy sameShape = new DefaultRateLimitPolicy("g", RateLimitPhase.PRE_AUTHENTICATION,
                RateLimit.of(40, Duration.ofMinutes(2), 20), 100);
        a.ramp(POLICY, 1, 2);
        // a chunk of 4 is claimed and one permit served from it
        assertAllowed(a.request(KEY, POLICY));
        assertThat(a.ctl.costs).containsExactly(1L, 2L, 4L);
        // served from the same chunk: a retired entry would start over with a claim of 1
        assertAllowed(a.request(KEY, sameShape));
        assertAllowed(a.request(KEY, POLICY));
        assertThat(a.ctl.costs.subList(3, a.ctl.costs.size())).doesNotContain(1L);
    }

    /**
     * An override can give a key a burst below {@code lease-size} and a long emission interval. Its lease time is that
     * of its largest chunk, {@code min(lease-size, burst)} intervals, which the key's burst tolerance bounds; with the
     * rule's {@code lease-size} it would overflow, and every chunk would be stranded the moment it arrived.
     */
    @Test
    void overriddenKeyLeasesForAtMostItsBurst() {
        Replica a = new Replica().leaseSize(10_000).build();
        RateLimitPolicy slow = new DefaultRateLimitPolicy("g", RateLimitPhase.PRE_AUTHENTICATION,
                RateLimit.of(1, Duration.ofDays(20), 1), 100);
        assertAllowed(a.request(KEY, slow));
        assertThat(a.ctl.costs).containsExactly(1L);
        assertThat(a.store.stranded()).isZero();
    }

    /**
     * A store may complete a claim on a thread of its own, for example a client library's. A claim that its reply
     * starts for the requests still waiting reaches the store on the context of the requests all the same.
     */
    @Test
    void followUpClaimRunsOnTheRequestContext() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            Context context = vertx.getOrCreateContext();
            Replica a = replica();
            a.ctl.mode = ControllableStore.Mode.HOLD;
            List<Context> calls = new CopyOnWriteArrayList<>();
            CountDownLatch followUp = new CountDownLatch(2);
            a.ctl.onCall = () -> {
                calls.add(Vertx.currentContext());
                followUp.countDown();
            };
            CountDownLatch queued = new CountDownLatch(1);
            context.runOnContext(v -> {
                // the first claim takes one permit, so the second request waits for another claim
                a.request();
                a.request();
                queued.countDown();
            });
            assertThat(queued.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(calls).containsExactly(context);

            Thread reply = new Thread(a.ctl::release, "foreign-store-reply");
            reply.start();
            reply.join(5_000);
            assertThat(followUp.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(calls.get(1)).isSameAs(context);
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    /**
     * A store that answers a claim with {@code null} has failed the call: the waiting request gets the
     * failure-mode decision, here {@code local}, and no second claim is made for it.
     */
    @Test
    void nullFromTheStoreIsAFailedClaim() {
        Replica a = replica();
        a.ctl.mode = ControllableStore.Mode.NULL_RESULT;
        assertAllowed(a.request());
        assertThat(a.ctl.costs).hasSize(1);
        assertThat(a.localRemaining(POLICY)).isEqualTo(POLICY.burst() - 1);
    }

    /**
     * Likewise, a key whose override has a burst below {@code max-overdraft} gets at most its burst of optimistic
     * admissions per window.
     */
    @Test
    void overriddenKeyOverdraftsAtMostItsBurst() {
        Replica a = new Replica().maxOverdraft(3).build();
        RateLimitPolicy one = policy(1, 1);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(3, one);
        assertThat(count(subs, Allowed.class)).isEqualTo(1);
    }

    // ---- provenance

    @Test
    void fallbackChunksAreSeparateAndDiscardedOnRecovery() {
        Replica a = replica();
        a.ramp(POLICY, 1, 2);
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        clock.advance(TIMEOUT);
        assertAllowed(waiter);
        // a fallback chunk of 4 (deadline send + T) served the waiter; the local bucket paid for it
        assertThat(a.store.held(KEY)).isEqualTo(3);
        assertThat(a.localRemaining(POLICY)).isEqualTo(16);
        a.ctl.mode = ControllableStore.Mode.PASS;
        assertAllowed(a.request());
        assertThat(a.ctl.costs).hasSize(3);
        // at 1 of 4 left the prefetch goes to the store; its grant drops the fallback chunk
        assertAllowed(a.request());
        assertThat(a.ctl.costs).hasSize(4);
        assertThat(a.store.fallbackDiscarded()).isEqualTo(1);
        assertThat(a.store.held(KEY)).isEqualTo(a.ctl.lastCost());

        // the same after a real OPEN → CLOSED cycle: with the breaker open nothing is prefetched, so the held store
        // permits run out and the next request claims (with a target well above 1)
        a.breaker.set(CircuitBreakerState.OPEN);
        while (a.store.held(KEY) > 0) {
            assertAllowed(a.request());
        }
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        waiter = a.request();
        clock.advance(TIMEOUT);
        assertAllowed(waiter);
        long fallbackHeld = a.store.held(KEY);
        assertThat(fallbackHeld).isPositive();
        a.breaker.set(CircuitBreakerState.CLOSED);
        a.ctl.mode = ControllableStore.Mode.PASS;
        long discardedBefore = a.store.fallbackDiscarded();
        assertAllowed(a.request());
        assertThat(a.store.fallbackDiscarded()).isEqualTo(discardedBefore + fallbackHeld);
    }

    @ParameterizedTest
    @MethodSource("failureModes")
    void fallbackSettlesReservedOverdraftLocally(FailureMode mode) {
        Replica a = new Replica().maxOverdraft(4).failureMode(mode).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        assertAllowed(a.request());
        assertAllowed(a.request());
        assertAllowed(a.request());
        assertAllowed(a.request());
        a.ctl.release();
        assertAllowed(a.request());
        assertThat(a.store.target(KEY)).isEqualTo(2);
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        assertThat(a.ctl.lastCost()).isEqualTo(5);
        assertThat(a.store.reserved(KEY)).isEqualTo(3);
        clock.advance(TIMEOUT);
        switch (mode) {
            case LOCAL -> {
                assertAllowed(waiter);
                assertThat(a.localRemaining(POLICY)).isEqualTo(15);
                assertThat(a.store.overdraft(Overdraft.SETTLED_FALLBACK)).isEqualTo(3);
                assertThat(a.store.held(KEY)).isEqualTo(1);
            }
            case OPEN -> {
                assertAllowed(waiter);
                assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(3);
            }
            case CLOSED -> {
                assertUnavailable(waiter);
                assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(3);
            }
        }
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(4);
        a.assertLedgerReconciles();
    }

    static Stream<FailureMode> failureModes() {
        return Stream.of(FailureMode.values());
    }

    @Test
    void heldAuthoritativeChunkSurvivesFailClosed() {
        RateLimitPolicy wide = policy(100, 100);
        Replica a = new Replica().failureMode(FailureMode.CLOSED).build();
        a.ramp(wide, 1, 2, 4);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> two = a.requests(2, wide);
        a.ctl.release();
        two.forEach(LeasingRateLimitStoreTest::assertAllowed);
        assertThat(a.store.held(KEY)).isEqualTo(6);
        a.ctl.mode = ControllableStore.Mode.FAIL;
        for (int i = 0; i < 6; i++) {
            assertAllowed(a.request(KEY, wide));
        }
        assertUnavailable(a.request(KEY, wide));
    }

    // ---- optimistic mode

    @Test
    void overdraftNeverWaits() {
        Replica a = new Replica().maxOverdraft(5).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        assertAllowed(a.request());
        assertThat(a.ctl.costs).containsExactly(2L);
        assertThat(a.store.reserved(KEY)).isEqualTo(1);
        for (int i = 0; i < 4; i++) {
            assertAllowed(a.request());
        }
        assertThat(a.store.unreserved(KEY)).isEqualTo(4);
        assertPending(a.request());
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(5);
    }

    @Test
    void admissionsDuringAClaimRideOnTheNext() {
        Replica a = new Replica().maxOverdraft(5).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 5; i++) {
            assertAllowed(a.request());
        }
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        a.ctl.release();
        assertAllowed(waiter);
        assertThat(a.store.overdraft(Overdraft.SETTLED)).isEqualTo(1);
        assertThat(a.store.unreserved(KEY)).isEqualTo(4);
        a.assertLedgerReconciles();
        // the next claim carries the 4
        waiter = a.request();
        assertThat(a.ctl.lastCost()).isEqualTo(2 + 4);
        assertThat(a.store.reserved(KEY)).isEqualTo(4);
        a.ctl.release();
        assertAllowed(waiter);
        assertThat(a.store.overdraft(Overdraft.SETTLED)).isEqualTo(5);
        assertThat(a.store.unreserved(KEY)).isZero();
        a.assertLedgerReconciles();
    }

    @Test
    void costNeverExceedsBurst() {
        RateLimitPolicy tiny = policy(4, 4);
        Replica a = new Replica().leaseSize(4).maxOverdraft(4).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 4; i++) {
            assertAllowed(a.request(KEY, tiny));
        }
        a.ctl.release();
        assertAllowed(a.request(KEY, tiny));
        assertThat(a.store.target(KEY)).isEqualTo(2);
        UniAssertSubscriber<RateLimitDecision> waiter = a.request(KEY, tiny);
        // the reservation is capped by burst - chunkPart: cost 4, never 5
        assertThat(a.ctl.lastCost()).isEqualTo(4);
        assertThat(a.store.reserved(KEY)).isEqualTo(3);
        a.ctl.release();
        // 2 left in the bucket: the claim of 4 is refused and forgiven, the single claim serves the waiter
        a.ctl.release();
        assertAllowed(waiter);
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(3);
        assertThat(a.ctl.costs).containsExactly(2L, 4L, 1L).allMatch(c -> c >= 1 && c <= 4);
        a.assertLedgerReconciles();
    }

    @Test
    void overdraftIsForgivenByARefusedClaim() {
        Replica a = new Replica().maxOverdraft(5).build();
        drainOracleTo(POLICY, 0);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 5; i++) {
            assertAllowed(a.request());
        }
        a.ctl.release();
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(1);
        assertThat(a.store.unreserved(KEY)).isEqualTo(4);
        a.ctl.mode = ControllableStore.Mode.PASS;
        clock.advance(T);
        // the entry timer settles the 4; the empty bucket refuses them
        assertThat(a.ctl.costs).containsExactly(2L, 4L);
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(5);
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(5);
        a.assertLedgerReconciles();
        // the cooldown holds: no optimistic admission, the request waits for the store (one permit regenerated)
        assertAllowed(a.request());
        assertThat(a.ctl.costs).containsExactly(2L, 4L, 1L);
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(5);
    }

    @Test
    void settlementWithoutFurtherTraffic() {
        Replica a = new Replica().maxOverdraft(4).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 4; i++) {
            assertAllowed(a.request());
        }
        a.ctl.release();
        assertThat(a.store.unreserved(KEY)).isEqualTo(3);
        a.ctl.mode = ControllableStore.Mode.PASS;
        clock.advance(T.minusMillis(1));
        assertThat(a.ctl.costs).hasSize(1);
        clock.advance(Duration.ofMillis(1));
        assertThat(a.ctl.costs).containsExactly(2L, 3L);
        assertThat(a.store.overdraft(Overdraft.SETTLED)).isEqualTo(4);
        assertThat(a.store.unreserved(KEY)).isZero();
        // no chunk was created by the settlement
        assertThat(a.store.held(KEY)).isEqualTo(1);
    }

    @Test
    void refusalWindowIsAtLeastT() {
        Replica a = new Replica().maxOverdraft(1).build();
        assertAllowed(a.request());
        assertAllowed(a.request());
        drainOracleTo(POLICY, 0);
        clock.advance(Duration.ofMillis(1800));
        // a cost-1 refusal with retryAfter 1.2 s = 0.4 T
        Rejected rejected = assertRejected(a.request());
        assertThat(rejected.retryAfter()).isEqualTo(Duration.ofMillis(1200));
        assertThat(a.ctl.costs).containsExactly(2L, 2L, 1L);
        clock.advance(Duration.ofMillis(1200));
        // 3 s after the first admission the window has ended, but the cooldown lasts T from the refusal
        assertAllowed(a.request());
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(1);
        clock.advance(Duration.ofMillis(1800));
        assertAllowed(a.request());
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(2);
    }

    @Test
    void resetEventsGrantAFreshBudgetAndAreCounted() {
        Replica a = new Replica().maxOverdraft(5).build();
        drainOracleTo(POLICY, 0);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int round = 0; round < 4; round++) {
            for (int i = 0; i < 5; i++) {
                assertAllowed(a.request());
            }
            assertPending(a.request());
            assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(5 * (round + 1));
            if (round < 3) {
                a.evictAll();
                assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(4 * (round + 1));
                assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(round + 1);
                a.assertLedgerReconciles();
            }
        }
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(20);

        // a policy change resets the budget too
        RateLimitPolicy other = policy(40, 40);
        for (int i = 0; i < 5; i++) {
            assertAllowed(a.request(KEY, other));
        }
        assertPending(a.request(KEY, other));
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(25);
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(16);
        assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(4);

        // and so does close() followed by a new decorator
        a.store.close();
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(20);
        assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(5);
        Replica fresh = new Replica().maxOverdraft(5).build();
        fresh.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 5; i++) {
            assertAllowed(fresh.request());
        }
        assertPending(fresh.request());
    }

    @ParameterizedTest
    @MethodSource("lateOutcomes")
    void lateGrantAfterEvictionSettlesOnlyTheReservedPart(String outcome) {
        Replica a = new Replica().maxOverdraft(4).failureMode(FailureMode.CLOSED).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 4; i++) {
            assertAllowed(a.request());
        }
        a.ctl.release();
        assertAllowed(a.request());
        if (outcome.equals("refused")) {
            drainOracleTo(POLICY, 0);
        }
        a.ctl.mode = outcome.equals("timeout") ? ControllableStore.Mode.DROP_REPLY : ControllableStore.Mode.HOLD;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        assertThat(a.ctl.lastCost()).isEqualTo(2 + 3);
        a.evictAll();
        assertUnavailable(waiter);
        assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(3);
        assertThat(a.store.unreconciledOutstanding()).isEqualTo(3);
        switch (outcome) {
            case "granted" -> {
                a.ctl.release();
                assertThat(a.store.overdraft(Overdraft.SETTLED_LATE)).isEqualTo(3);
                assertThat(a.store.stranded()).isEqualTo(2);
                assertThat(a.store.unreconciledOutstanding()).isZero();
            }
            case "refused" -> {
                a.ctl.release();
                assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(3);
                assertThat(a.store.stranded()).isZero();
                assertThat(a.store.unreconciledOutstanding()).isZero();
            }
            case "timeout" -> {
                clock.advance(TIMEOUT);
                assertThat(a.store.unreconciledOutstanding()).isEqualTo(3);
                assertThat(a.store.stranded()).isZero();
            }
            default -> throw new IllegalArgumentException(outcome);
        }
        // the entry counter is monotonic: it still says 3 went through the unreconciled state
        assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(3);
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(4);
        assertThat(a.store.overdraft(Overdraft.SETTLED)).isEqualTo(1);
        a.assertLedgerReconciles();
    }

    static Stream<String> lateOutcomes() {
        return Stream.of("granted", "refused", "timeout");
    }

    @Test
    void delayedRefusalCooldown() {
        RateLimitPolicy perSecond = policy(60, 60);
        Replica a = new Replica().maxOverdraft(1).timeout(Duration.ofSeconds(2)).build();
        // 1.6 permits available: a claim of 2 is refused with retryAfter 0.4 s
        drainOracleTo(perSecond, 0);
        clock.advance(Duration.ofMillis(1600));
        a.ctl.mode = ControllableStore.Mode.HOLD;
        assertAllowed(a.request(KEY, perSecond));
        clock.advance(Duration.ofMillis(900));
        a.ctl.release();
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(1);
        a.ctl.mode = ControllableStore.Mode.PASS;
        clock.advance(Duration.ofMillis(900));
        // 1.8 s: the window has ended but the cooldown runs to 0.9 + max(1, 0.4) = 1.9 s
        assertAllowed(a.request(KEY, perSecond));
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(1);
        clock.advance(Duration.ofMillis(100));
        assertAllowed(a.request(KEY, perSecond));
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(2);
    }

    @Test
    void silenceStillSettles() {
        Replica a = new Replica().maxOverdraft(4).build();
        a.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 4; i++) {
            assertAllowed(a.request());
        }
        UniAssertSubscriber<RateLimitDecision> last = a.request();
        last.cancel();
        a.ctl.release();
        a.ctl.mode = ControllableStore.Mode.PASS;
        assertThat(a.store.unreserved(KEY)).isEqualTo(3);
        clock.advance(T);
        assertThat(a.ctl.costs).containsExactly(2L, 3L);
        assertThat(a.store.overdraft(Overdraft.SETTLED)).isEqualTo(4);

        // a policy change before the timer fires cancels the settlement and forgives the admissions
        Replica b = new Replica().maxOverdraft(4).build();
        b.ctl.mode = ControllableStore.Mode.HOLD;
        for (int i = 0; i < 4; i++) {
            assertAllowed(b.request());
        }
        b.ctl.release();
        b.ctl.mode = ControllableStore.Mode.PASS;
        clock.advance(Duration.ofSeconds(1));
        assertAllowed(b.request(KEY, policy(40, 40)));
        assertThat(b.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(3);
        clock.advance(T);
        // the new policy's claim only; no SETTLE under the old tag
        assertThat(b.ctl.costs).containsExactly(2L, 2L);
        b.assertLedgerReconciles();
    }

    @Test
    void chainedClaimsKeepOneInFlight() {
        Replica a = replica();
        a.ramp(POLICY, 1);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(5, POLICY);
        assertThat(a.ctl.lastCost()).isEqualTo(2);
        // a request that arrives while the replacement claim is being issued (phase 2)
        List<UniAssertSubscriber<RateLimitDecision>> concurrent = new ArrayList<>();
        a.ctl.onCall = () -> concurrent.add(a.request());
        a.ctl.release();
        a.ctl.onCall = () -> {
        };
        assertThat(count(subs, Allowed.class)).isEqualTo(2);
        assertThat(a.ctl.held).hasSize(1);
        assertThat(concurrent).hasSize(1);
        assertPending(concurrent.getFirst());
        assertThat(a.ctl.maxOutstanding).isEqualTo(1);
        while (!a.ctl.held.isEmpty()) {
            a.ctl.release();
        }
        subs.forEach(LeasingRateLimitStoreTest::assertAllowed);
        assertAllowed(concurrent.getFirst());
        assertThat(a.ctl.maxOutstanding).isEqualTo(1);
    }

    @Test
    void refusedFullClaimThenDelayedSingleClaim() {
        Replica a = new Replica().maxOverdraft(2).build();
        assertAllowed(a.request());
        assertAllowed(a.request());
        assertThat(a.store.target(KEY)).isEqualTo(2);
        drainOracleTo(POLICY, 1);
        clock.advance(Duration.ofMillis(2970));
        a.ctl.mode = ControllableStore.Mode.HOLD;
        assertAllowed(a.request());
        assertThat(a.ctl.lastCost()).isEqualTo(3);
        UniAssertSubscriber<RateLimitDecision> waiter = a.request();
        clock.advance(Duration.ofMillis(20));
        a.ctl.release();
        // the full claim is refused and forgiven; a single claim replaces it
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(1);
        assertThat(a.ctl.lastCost()).isEqualTo(1);
        assertPending(waiter);
        clock.advance(Duration.ofMillis(10));
        // a new window: arrivals during the single claim stay unreserved
        assertAllowed(a.request());
        assertAllowed(a.request());
        assertThat(a.store.unreserved(KEY)).isEqualTo(2);
        clock.advance(Duration.ofMillis(20));
        a.ctl.release();
        assertAllowed(waiter);
        assertThat(a.store.held(KEY)).isZero();
        a.assertLedgerReconciles();
    }

    @Test
    void synchronousDelegateDoesNotReenterCompute() {
        Replica a = replica();
        assertAllowed(a.request());
        drainOracleTo(POLICY, 0);
        assertRejected(a.request());
        a.ctl.mode = ControllableStore.Mode.FAIL;
        clock.advance(T);
        // the local fallback completes synchronously too (claim of 2: target doubled after the first chunk)
        assertAllowed(a.request());
        assertThat(a.localRemaining(POLICY)).isEqualTo(18);
    }

    @ParameterizedTest
    @MethodSource("outcomeTable")
    void outcomeTableIsExhaustive(String kind, String outcome) {
        FailureMode mode = switch (outcome) {
            case "open" -> FailureMode.OPEN;
            case "closed" -> FailureMode.CLOSED;
            default -> FailureMode.LOCAL;
        };
        Replica a = new Replica().maxOverdraft(8).failureMode(mode).build();
        switch (kind) {
            case "on-demand", "late" -> {
                // claim #3 carries r1 and r3: chunk part 2, reserved 2
                a.ctl.mode = ControllableStore.Mode.HOLD;
                assertAllowed(a.request());
                assertAllowed(a.request());
                a.ctl.release();
                assertAllowed(a.request());
                prepare(a, outcome);
                assertAllowed(a.request());
            }
            case "prefetch" -> {
                for (int i = 0; i < 5; i++) {
                    assertAllowed(a.request());
                }
                a.ctl.mode = ControllableStore.Mode.HOLD;
                assertAllowed(a.request());
                assertAllowed(a.request());
                assertAllowed(a.request());
                a.ctl.release();
                assertThat(a.store.held(KEY)).isEqualTo(4);
                assertAllowed(a.request());
                assertAllowed(a.request());
                prepare(a, outcome);
                // 1 of 4 left: the prefetch carries the 2 unreserved
                assertAllowed(a.request());
                assertAllowed(a.request());
            }
            case "settle" -> {
                a.ctl.mode = ControllableStore.Mode.HOLD;
                assertAllowed(a.request());
                assertAllowed(a.request());
                assertAllowed(a.request());
                a.ctl.release();
                prepare(a, outcome);
                clock.advance(T);
                assertAllowed(a.request());
            }
            default -> throw new IllegalArgumentException(kind);
        }
        assertThat(a.store.reserved(KEY)).isEqualTo(2);
        for (int i = 0; i < 3; i++) {
            assertAllowed(a.request());
        }
        assertThat(a.store.unreserved(KEY)).isEqualTo(3);
        long admitted = a.store.overdraft(Overdraft.ADMITTED);
        a.assertLedgerReconciles();
        if (kind.equals("late")) {
            a.evictAll();
            assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(2);
            assertThat(a.store.unreconciledOutstanding()).isEqualTo(2);
            assertThat(a.store.unreserved(KEY)).isZero();
        }

        switch (outcome) {
            case "granted", "refused" -> a.ctl.release();
            default -> clock.advance(TIMEOUT);
        }

        Overdraft terminal = switch (outcome) {
            case "granted" -> kind.equals("late") ? Overdraft.SETTLED_LATE : Overdraft.SETTLED;
            case "refused" -> Overdraft.FORGIVEN;
            case "local" -> Overdraft.SETTLED_FALLBACK;
            default -> kind.equals("late") ? Overdraft.UNRECONCILED : Overdraft.FORGIVEN;
        };
        assertThat(a.store.overdraft(terminal)).as(terminal.name()).isGreaterThanOrEqualTo(2);
        if (kind.equals("late")) {
            assertThat(a.store.unreconciledOutstanding()).isEqualTo(terminal == Overdraft.UNRECONCILED ? 2 : 0);
        }
        assertThat(a.store.reserved(KEY)).isZero();
        assertThat(a.store.unreserved(KEY)).isEqualTo(kind.equals("late") ? 0 : 3);
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(admitted);
        a.assertLedgerReconciles();
    }

    private void prepare(Replica a, String outcome) {
        switch (outcome) {
            case "granted" -> a.ctl.mode = ControllableStore.Mode.HOLD;
            case "refused" -> {
                drainOracleTo(POLICY, 0);
                a.ctl.mode = ControllableStore.Mode.HOLD;
            }
            default -> a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        }
    }

    static Stream<Arguments> outcomeTable() {
        List<Arguments> cases = new ArrayList<>();
        for (String kind : List.of("on-demand", "prefetch", "settle", "late")) {
            for (String outcome : List.of("granted", "refused", "local", "open", "closed")) {
                cases.add(Arguments.of(kind, outcome));
            }
        }
        return cases.stream();
    }

    @Test
    void expiredFallbackGrantAnswersCoveredWaitersOnce() {
        // T = 50 ms, lease time 400 ms, store timeout 100 ms
        RateLimitPolicy fast = policy(1_200, 1_200);
        Replica a = replica();
        a.ramp(fast, 1, 2);
        a.ctl.mode = ControllableStore.Mode.HOLD;
        List<UniAssertSubscriber<RateLimitDecision>> three = a.requests(3, fast);
        assertThat(a.ctl.lastCost()).isEqualTo(4);
        a.ctl.release();
        three.forEach(LeasingRateLimitStoreTest::assertAllowed);
        clock.advance(Duration.ofMillis(400));
        // the chunk expired with 1 left: target 3
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        List<UniAssertSubscriber<RateLimitDecision>> covered = a.requests(2, fast);
        assertThat(a.ctl.lastCost()).isEqualTo(3);
        assertThat(a.store.stranded()).isEqualTo(1);
        clock.advance(TIMEOUT);
        // the fallback grant's deadline (send + T = 50 ms) has passed: it still answers the 2 covered waiters once
        covered.forEach(LeasingRateLimitStoreTest::assertAllowed);
        assertThat(a.store.fallbackDiscarded()).isEqualTo(1);
        assertThat(a.localRemaining(fast)).isEqualTo(1_200 - 3);
        assertThat(a.store.held(KEY)).isZero();
    }

    @Test
    void overdraftScalesWithReplicasByDesign() {
        drainOracleTo(POLICY, 0);
        List<Replica> replicas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Replica r = new Replica().maxOverdraft(5).build();
            r.ctl.mode = ControllableStore.Mode.DELAY;
            r.ctl.delay = Duration.ofMillis(10);
            replicas.add(r);
        }
        long admitted = 0;
        for (Replica r : replicas) {
            for (int i = 0; i < 5; i++) {
                assertAllowed(r.request());
            }
            assertPending(r.request());
            admitted += r.store.overdraft(Overdraft.ADMITTED);
        }
        assertThat(admitted).isEqualTo(15);
        clock.advance(Duration.ofMillis(10));
        for (Replica r : replicas) {
            r.ctl.mode = ControllableStore.Mode.PASS;
        }
        clock.advance(TIMEOUT);
        long forgiven = 0;
        for (Replica r : replicas) {
            // the waiter's own claim was refused too: its reservation rides along and is forgiven
            assertRejected(r.request());
            forgiven += r.store.overdraft(Overdraft.FORGIVEN);
            assertThat(r.store.overdraft(Overdraft.ADMITTED)).isEqualTo(5);
            r.assertLedgerReconciles();
        }
        assertThat(forgiven).isEqualTo(15);
    }

    @Test
    void strictModeNeverOverAdmits() {
        drainOracleTo(POLICY, 0);
        Replica a = replica();
        a.ctl.mode = ControllableStore.Mode.DELAY;
        a.ctl.delay = Duration.ofMillis(10);
        List<UniAssertSubscriber<RateLimitDecision>> subs = a.requests(5, POLICY);
        subs.forEach(LeasingRateLimitStoreTest::assertPending);
        clock.advance(Duration.ofMillis(10));
        subs.forEach(LeasingRateLimitStoreTest::assertRejected);
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isZero();
        assertThat(((Rejected) subs.getFirst().getItem()).retryAfter()).isEqualTo(T);
    }

    // ---- lifecycle

    @Test
    void equalOverrideDoesNotReset() {
        Replica a = new Replica().maxOverdraft(1).build();
        RateLimitPolicy same = policy(20, 20);
        assertThat(same).isEqualTo(POLICY).isNotSameAs(POLICY);
        assertAllowed(a.request(KEY, POLICY));
        assertAllowed(a.request(KEY, same));
        assertAllowed(a.request(KEY, same));
        assertThat(a.ctl.costs).containsExactly(2L, 2L);
        assertThat(a.store.stranded()).isZero();
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isZero();
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(1);
    }

    @Test
    void firstWaiterGetsTheClaimOutcomeNotATimerFallback() {
        RateLimitPolicy one = policy(1, 1);
        Replica a = new Replica().leaseSize(1).build();
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        UniAssertSubscriber<RateLimitDecision> waiter = a.request(KEY, one);
        clock.advance(TIMEOUT.minusMillis(1));
        assertPending(waiter);
        clock.advance(Duration.ofMillis(1));
        assertAllowed(waiter);
        assertThat(a.localRemaining(one)).isZero();
        clock.advance(Duration.ofSeconds(1));
        assertThat(a.localRemaining(one)).isZero();
    }

    @Test
    void uncoveredWaiterIsAnsweredAtItsDeadline() {
        Replica a = new Replica().leaseSize(1).build();
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        UniAssertSubscriber<RateLimitDecision> w1 = a.request();
        clock.advance(Duration.ofMillis(5));
        UniAssertSubscriber<RateLimitDecision> w2 = a.request();
        clock.advance(Duration.ofMillis(95));
        assertAllowed(w1);
        assertPending(w2);
        assertThat(a.localRemaining(POLICY)).isEqualTo(19);
        clock.advance(Duration.ofMillis(5));
        assertAllowed(w2);
        assertThat(a.localRemaining(POLICY)).isEqualTo(18);
        // claim #2's fallback later debits nothing: its waiter is gone
        clock.advance(TIMEOUT);
        assertThat(a.localRemaining(POLICY)).isEqualTo(18);
        assertThat(a.store.held(KEY)).isZero();
    }

    @Test
    void claimWithNoRemainingWaitersDebitsNothingLocally() {
        Replica a = new Replica().leaseSize(2).build();
        a.ctl.mode = ControllableStore.Mode.DROP_REPLY;
        UniAssertSubscriber<RateLimitDecision> w1 = a.request();
        clock.advance(Duration.ofMillis(5));
        UniAssertSubscriber<RateLimitDecision> w2 = a.request();
        clock.advance(Duration.ofMillis(95));
        // claim #1's fallback (cost 1) serves w1; the chained claim #2 is sent for w2
        assertAllowed(w1);
        assertThat(a.ctl.costs).containsExactly(1L, 2L);
        clock.advance(Duration.ofMillis(5));
        assertAllowed(w2);
        assertThat(a.localRemaining(POLICY)).isEqualTo(18);
        clock.advance(TIMEOUT);
        assertThat(a.localRemaining(POLICY)).isEqualTo(18);
        assertThat(a.store.held(KEY)).isZero();
    }

    @Test
    void evictionOnlyStrands() {
        Replica a = replica();
        a.ramp(POLICY, 1, 2);
        assertAllowed(a.request());
        assertAllowed(a.request());
        a.ctl.mode = ControllableStore.Mode.HOLD;
        assertAllowed(a.request());
        // 1 of 4 left: the prefetch is in flight
        assertThat(a.ctl.held).hasSize(1);
        long prefetchCost = a.ctl.lastCost();
        int calls = a.ctl.costs.size();
        a.evictAll();
        assertThat(a.store.stranded()).isEqualTo(1);
        assertThat(a.ctl.costs).hasSize(calls);
        a.ctl.release();
        assertThat(a.store.stranded()).isEqualTo(1 + prefetchCost);
        assertThat(a.store.trackedKeys()).isZero();
    }

    @Test
    void policyChangeStrandsAndReclaims() {
        Replica a = replica();
        a.ramp(POLICY, 1, 2);
        assertAllowed(a.request());
        assertThat(a.store.held(KEY)).isEqualTo(3);
        RateLimitPolicy other = policy(40, 40);
        assertAllowed(a.request(KEY, other));
        assertThat(a.store.stranded()).isEqualTo(3);
        assertThat(a.ctl.lastCost()).isEqualTo(1);
        assertThat(a.store.target(KEY)).isEqualTo(2);
        assertThat(a.store.held(KEY)).isZero();
    }

    @Test
    void closeFailsWaitersAndStrands() {
        Replica a = new Replica().maxOverdraft(2).failureMode(FailureMode.CLOSED).build();
        // one key holds a chunk of 2
        assertAllowed(a.request());
        assertAllowed(a.request());
        assertAllowed(a.request());
        assertThat(a.store.held(KEY)).isEqualTo(2);
        // another key: a claim held, 2 overdraft admissions, 2 waiters
        a.ctl.mode = ControllableStore.Mode.HOLD;
        assertAllowed(a.request("other", POLICY));
        assertAllowed(a.request("other", POLICY));
        UniAssertSubscriber<RateLimitDecision> w1 = a.request("other", POLICY);
        UniAssertSubscriber<RateLimitDecision> w2 = a.request("other", POLICY);
        assertPending(w1);
        assertPending(w2);

        a.store.close();
        assertUnavailable(w1);
        assertUnavailable(w2);
        assertThat(a.store.stranded()).isEqualTo(2);
        assertThat(a.store.overdraft(Overdraft.ADMITTED)).isEqualTo(4);
        assertThat(a.store.overdraft(Overdraft.SETTLED)).isEqualTo(2);
        assertThat(a.store.overdraft(Overdraft.FORGIVEN)).isEqualTo(1);
        assertThat(a.store.overdraft(Overdraft.UNRECONCILED)).isEqualTo(1);
        assertThat(a.store.trackedKeys()).isZero();
    }
}
