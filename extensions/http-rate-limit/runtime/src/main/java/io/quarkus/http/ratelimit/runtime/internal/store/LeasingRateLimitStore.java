package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.quarkus.http.ratelimit.runtime.internal.store.GuardedRateLimitStore.Granted;
import io.quarkus.http.ratelimit.runtime.internal.store.GuardedRateLimitStore.Source;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Lease mode ({@code lease-size} set): claims chunks of permits from the store with one conditional call each and serves
 * requests
 * from the chunk until a deadline fixed at the claim's send time. The store debits permits before they are served, so
 * a lost reply strands permits but never serves them twice; the number of replicas is never known or assumed.
 * <p>
 * Every state change of a key happens inside {@code cache.asMap().compute(key, ...)} (phase 1) and only collects
 * effects; the store call, the waiters' completions and the fallback decisions run after the compute returns (phase
 * 2), because a synchronously completing {@link Uni} would otherwise re-enter the compute for the same key. Each
 * claim carries a unique token, and a reply only ever retires the claim it belongs to.
 * <p>
 * The counters are exposed as plain accessors; the metrics bean reads them, so this class knows nothing of Micrometer.
 */
public final class LeasingRateLimitStore implements RateLimitStore, AutoCloseable {

    private static final Logger LOG = Logger.getLogger(LeasingRateLimitStore.class);

    static final int MAX_WAITERS = 1_000;
    /**
     * Marks an unset instant; {@link RateLimitClock#nanoTime()} has an arbitrary origin, so instants are only ever
     * compared by difference.
     */
    private static final long NONE = Long.MIN_VALUE;

    /**
     * The categories of an optimistic admission ({@code max-overdraft > 0}): every admission is counted once as
     * {@link #ADMITTED} and once in exactly one other category; an admission counted as {@link #UNRECONCILED} is
     * counted again under its settled or forgiven category if a late reply tells its outcome.
     */
    public enum Overdraft {
        ADMITTED("admitted"),
        SETTLED("settled"),
        SETTLED_FALLBACK("settled_fallback"),
        SETTLED_LATE("settled_late"),
        FORGIVEN("forgiven"),
        /**
         * The replica lost the key's entry before the claim's outcome arrived. Counts entries into the state, so it is
         * monotonic like the other categories; {@link #unreconciledOutstanding()} is what is still unresolved, which
         * drops when a late outcome settles or forgives the admissions and stays when the outcome says nothing about
         * the store's debit.
         */
        UNRECONCILED("unreconciled");

        private final String tag;

        Overdraft(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    enum Kind {
        ON_DEMAND,
        PREFETCH,
        SETTLE
    }

    /**
     * An immutable snapshot of a claim at its send time; {@code cost = chunkPart + reserved}.
     */
    record Claim(Kind kind, long chunkPart, long reserved, long sendNanos, long token) {

        long cost() {
            return chunkPart + reserved;
        }
    }

    /**
     * A grant being served; {@code left} is the only mutable part.
     */
    static final class Chunk {
        final long size;
        final long deadlineNanos;
        final Source source;
        final boolean grantedWhileDegraded;
        long left;

        Chunk(long size, long deadlineNanos, Source source, boolean grantedWhileDegraded) {
            this.size = size;
            this.left = size;
            this.deadlineNanos = deadlineNanos;
            this.source = source;
            this.grantedWhileDegraded = grantedWhileDegraded;
        }
    }

    private static final class Waiter {
        final long deadlineNanos;
        final UniEmitter<? super RateLimitDecision> emitter;
        volatile boolean done;

        Waiter(long deadlineNanos, UniEmitter<? super RateLimitDecision> emitter) {
            this.deadlineNanos = deadlineNanos;
            this.emitter = emitter;
        }
    }

    /**
     * The effect that answers a request with a decision taken in phase 1.
     */
    private record Complete(UniEmitter<? super RateLimitDecision> emitter, RateLimitDecision decision) implements Runnable {

        @Override
        public void run() {
            emitter.complete(decision);
        }
    }

    private static final class Entry {
        RateLimitPolicy policy;
        final ArrayDeque<Chunk> chunks = new ArrayDeque<>(2);
        Claim inFlight;
        // the chunk part the in-flight claim's failure-mode fallback debits locally (0 once its waiters are gone)
        long fallbackChunkPart;
        long target = 1;
        long lastRemaining = NONE;
        long refusedUntilNanos = NONE;
        long prefetchRefusedUntilNanos = NONE;
        final ArrayDeque<Waiter> waiters = new ArrayDeque<>();
        long unreserved;
        long windowAdmitted;
        long windowStartNanos = NONE;
        long cooldownUntilNanos = NONE;
        long lastAdmissionNanos;
        // one lease time past the latest chunk's deadline, so that the adaptive target survives an idle spell
        long retainUntilNanos = NONE;
        RateLimitClock.Cancellable timer;
        // the Vert.x context of the latest request, on which claims call the store whatever thread starts them
        Context context;
        // the due time the timer is armed for, meaningful while timer is set
        long timerDueNanos;
        long timerSerial;

        Entry(RateLimitPolicy policy) {
            this.policy = policy;
        }

        long reserved() {
            return inFlight == null ? 0 : inFlight.reserved;
        }

        long heldStore() {
            long held = 0;
            for (Chunk c : chunks) {
                if (c.source == Source.STORE) {
                    held += c.left;
                }
            }
            return held;
        }
    }

    private final GuardedRateLimitStore delegate;
    private final long leaseSize;
    private final long maxOverdraft;
    private final long waiterBudgetNanos;
    private final RateLimitClock clock;
    private final Cache<String, Entry> cache;
    private final AtomicLong tokens = new AtomicLong();
    // effects collected by the eviction listener, which runs inside Caffeine's own locks
    private final Queue<Runnable> evicted = new ConcurrentLinkedQueue<>();

    private final LongAdder claimsGranted = new LongAdder();
    private final LongAdder claimsRefused = new LongAdder();
    private final LongAdder stranded = new LongAdder();
    private final LongAdder fallbackDiscarded = new LongAdder();
    private final Map<Overdraft, LongAdder> overdraft = new EnumMap<>(Overdraft.class);
    private final LongAdder unreconciledOutstanding = new LongAdder();

    /**
     * @param leaseSize the largest chunk one claim may take; a chunk is served for {@code leaseSize} emission
     *        intervals
     * @param maxOverdraft the requests per key admitted per emission interval while no chunk is held, without waiting
     *        for the claim they start
     * @param maxKeys the most keys tracked for this rule
     * @param waiterBudget how long a request may wait for a claim: the store timeout, which also bounds each claim
     * @param customizer applies the {@code RateLimitLocalStoreCustomizer}s to the cache, after its own settings
     */
    LeasingRateLimitStore(GuardedRateLimitStore delegate,
            long leaseSize,
            long maxOverdraft,
            long maxKeys,
            Duration waiterBudget,
            RateLimitClock clock,
            Consumer<Caffeine<Object, Object>> customizer) {
        this.delegate = delegate;
        this.leaseSize = leaseSize;
        this.maxOverdraft = maxOverdraft;
        this.waiterBudgetNanos = waiterBudget.toNanos();
        this.clock = clock;
        for (Overdraft o : Overdraft.values()) {
            overdraft.put(o, new LongAdder());
        }
        this.cache = KeyCaches.build(maxKeys, new EntryExpiry(), new RemovalListener<>() {
            @Override
            public void onRemoval(String key, Entry entry, RemovalCause cause) {
                retire(key, entry, evicted);
            }
        }, clock, customizer);
    }

    // ---- accessors

    /**
     * @return the number of keys this replica holds lease state for
     */
    public long trackedKeys() {
        cache.cleanUp();
        run(evicted);
        return cache.estimatedSize();
    }

    /**
     * @return the permits held in chunks for the key, for tests and diagnostics
     */
    long held(String key) {
        Entry e = cache.asMap().get(key);
        if (e == null) {
            return 0;
        }
        long held = 0;
        for (Chunk c : e.chunks) {
            held += c.left;
        }
        return held;
    }

    public long claims(boolean granted) {
        return (granted ? claimsGranted : claimsRefused).sum();
    }

    /**
     * @return store-granted permits this replica never served
     */
    public long stranded() {
        return stranded.sum();
    }

    /**
     * @return locally granted fallback permits dropped unserved
     */
    public long fallbackDiscarded() {
        return fallbackDiscarded.sum();
    }

    /**
     * @return the admissions counted into the category so far; never decreases
     */
    public long overdraft(Overdraft category) {
        return overdraft.get(category).sum();
    }

    /**
     * @return the unreconciled admissions whose outcome is still unknown
     */
    public long unreconciledOutstanding() {
        return unreconciledOutstanding.sum();
    }

    // for tests
    long unreserved(String key) {
        Entry e = cache.asMap().get(key);
        return e == null ? 0 : e.unreserved;
    }

    long reserved(String key) {
        Entry e = cache.asMap().get(key);
        return e == null ? 0 : e.reserved();
    }

    long target(String key) {
        Entry e = cache.asMap().get(key);
        return e == null ? 1 : e.target;
    }

    Cache<String, ?> cache() {
        return cache;
    }

    // ---- the request path

    @Override
    public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
        if (cost == 0) {
            return delegate.tryConsume(key, 0, policy);
        }
        if (cost != 1) {
            throw new IllegalArgumentException("Rate limit " + policy.id() + ": lease mode serves one permit per call");
        }
        return Uni.createFrom().emitter(new Consumer<>() {
            @Override
            public void accept(UniEmitter<? super RateLimitDecision> em) {
                Context context = Vertx.currentContext();
                transition(key, new Step() {
                    @Override
                    public Entry apply(String k, Entry e, List<Runnable> fx) {
                        long now = clock.nanoTime();
                        if (e == null) {
                            e = new Entry(policy);
                        } else if (!e.policy.equals(policy)) {
                            // held permits depend only on the emission interval and the burst: a policy that keeps
                            // both keeps the entry, so that an override alternating between such limits strands
                            // nothing; any other change is exactly like a removal followed by a fresh entry
                            if (e.policy.emissionIntervalNanos() != policy.emissionIntervalNanos()
                                    || e.policy.burst() != policy.burst()) {
                                retire(k, e, fx);
                            }
                            e.policy = policy;
                        }
                        if (context != null) {
                            e.context = context;
                        }
                        arrive(k, e, now, em, fx);
                        tickAndRearm(k, e, now, fx);
                        return e;
                    }
                });
            }
        });
    }

    private void arrive(String key, Entry e, long now, UniEmitter<? super RateLimitDecision> em, List<Runnable> fx) {
        dropDeadChunks(e, now);
        Chunk chunk = e.chunks.peekFirst();
        if (chunk != null) {
            // held permits are always served before any refusal cache is consulted
            fx.add(new Complete(em, serve(e, chunk)));
            maybePrefetch(key, e, now, fx);
            return;
        }
        if (e.refusedUntilNanos != NONE && e.refusedUntilNanos - now > 0) {
            fx.add(new Complete(em, RateLimitDecision.rejected(Duration.ofNanos(e.refusedUntilNanos - now))));
            return;
        }
        if (maxOverdraft > 0 && overdraftOpen(e, now)) {
            e.unreserved++;
            e.windowAdmitted++;
            e.lastAdmissionNanos = now;
            overdraft.get(Overdraft.ADMITTED).increment();
            fx.add(new Complete(em, RateLimitDecision.allowed(estimate(e))));
            if (e.inFlight == null) {
                claim(key, e, Kind.ON_DEMAND, now, fx);
            }
            return;
        }
        if (e.waiters.size() >= MAX_WAITERS) {
            // the store is healthy, so the failure mode does not apply: open would admit every request past the cap
            fx.add(answer(em, delegate.localDecision(key, e.policy)));
            return;
        }
        Waiter waiter = new Waiter(now + waiterBudgetNanos, em);
        e.waiters.add(waiter);
        // registered in phase 2: an already cancelled emitter runs the callback at once
        fx.add(new Runnable() {
            @Override
            public void run() {
                em.onTermination(new Runnable() {
                    @Override
                    public void run() {
                        cancel(key, waiter);
                    }
                });
            }
        });
        if (e.inFlight == null) {
            claim(key, e, Kind.ON_DEMAND, now, fx);
        }
    }

    /**
     * Opens or renews the overdraft window and says whether it has budget left.
     */
    private boolean overdraftOpen(Entry e, long now) {
        if (e.cooldownUntilNanos != NONE && e.cooldownUntilNanos - now > 0) {
            return false;
        }
        long window = e.policy.emissionIntervalNanos();
        if (e.windowStartNanos == NONE || now - e.windowStartNanos >= window) {
            e.windowStartNanos = now;
            e.windowAdmitted = 0;
        }
        // an override can give the key a burst below max-overdraft
        return e.windowAdmitted < Math.min(maxOverdraft, e.policy.burst());
    }

    private RateLimitDecision serve(Entry e, Chunk chunk) {
        chunk.left--;
        if (chunk.left == 0) {
            e.chunks.remove(chunk);
            // fully used before its deadline: grow
            e.target = Math.clamp(2 * chunk.size, e.target, leaseSize);
        }
        return RateLimitDecision.allowed(estimate(e));
    }

    /**
     * The store's last remaining plus the permits held in store chunks: an estimate of the shared remaining.
     */
    private long estimate(Entry e) {
        long burst = e.policy.burst();
        if (e.lastRemaining == NONE) {
            return burst;
        }
        return Math.clamp(e.lastRemaining + e.heldStore(), 0, burst);
    }

    private void maybePrefetch(String key, Entry e, long now, List<Runnable> fx) {
        Chunk chunk = e.chunks.peekFirst();
        if (e.chunks.size() == 1 && e.inFlight == null && chunk.left <= chunk.size / 4
                && !(e.prefetchRefusedUntilNanos != NONE && e.prefetchRefusedUntilNanos - now > 0)
                && !delegate.degraded()) {
            claim(key, e, Kind.PREFETCH, now, fx);
        }
    }

    /**
     * Drops expired chunks (store permits left in them are stranded) and fallback chunks the breaker's recovery
     * invalidated.
     */
    private void dropDeadChunks(Entry e, long now) {
        Iterator<Chunk> it = e.chunks.iterator();
        while (it.hasNext()) {
            Chunk c = it.next();
            if (c.deadlineNanos - now <= 0) {
                it.remove();
                if (c.source == Source.STORE) {
                    stranded.add(c.left);
                    e.target = Math.max(1, c.size - c.left);
                } else {
                    fallbackDiscarded.add(c.left);
                }
            } else if (c.source == Source.LOCAL_FALLBACK && c.grantedWhileDegraded && !delegate.degraded()) {
                it.remove();
                fallbackDiscarded.add(c.left);
            }
        }
    }

    // ---- claims

    /**
     * An on-demand or prefetch claim sized by the adaptive target, the lease size, the tail cap and the burst.
     */
    private void claim(String key, Entry e, Kind kind, long now, List<Runnable> fx) {
        claim(key, e, kind, Math.min(Math.min(e.target, leaseSize), Math.min(tailCap(e), e.policy.burst())), now, fx);
    }

    private void claim(String key, Entry e, Kind kind, long chunkPart, long now, List<Runnable> fx) {
        long burst = e.policy.burst();
        long reserved = Math.min(e.unreserved, burst - chunkPart);
        e.unreserved -= reserved;
        long cost = chunkPart + reserved;
        if (cost < 1 || cost > burst) {
            throw new IllegalStateException("Rate limit " + e.policy.id() + ": claim cost " + cost + " outside [1, "
                    + burst + "]");
        }
        Claim claim = new Claim(kind, chunkPart, reserved, now, tokens.incrementAndGet());
        e.inFlight = claim;
        RateLimitPolicy policy = e.policy;
        Context context = e.context;
        Runnable call = new Runnable() {
            @Override
            public void run() {
                delegate.attempt(key, cost, policy).subscribe().with(
                        new Consumer<>() {
                            @Override
                            public void accept(RateLimitDecision decision) {
                                onReply(key, claim, new Granted(decision, Source.STORE));
                            }
                        },
                        new Consumer<>() {
                            @Override
                            public void accept(Throwable failure) {
                                onFailure(key, claim, policy, failure);
                            }
                        });
            }
        };
        fx.add(new Runnable() {
            @Override
            public void run() {
                // a reply that completes on a thread of the store's own can start this claim: the store is still
                // called on the context of the key's requests
                if (context == null || Vertx.currentContext() == context) {
                    call.run();
                } else {
                    context.runOnContext(new Handler<>() {
                        @Override
                        public void handle(Void ignored) {
                            call.run();
                        }
                    });
                }
            }
        });
    }

    /**
     * Never more than half of the estimated shared remaining (what the store last said was left plus the store permits
     * held here), and at least 1: a stale, local heuristic that keeps one replica from reserving the whole tail.
     */
    private long tailCap(Entry e) {
        if (e.lastRemaining == NONE) {
            return leaseSize;
        }
        return Math.max(1, (e.lastRemaining + e.heldStore()) / 2);
    }

    /**
     * The guarded call failed: the failure-mode decision is taken for the reserved admissions plus, if the claim still
     * has waiters, its chunk part; a claim left without waiters debits nothing locally.
     */
    private void onFailure(String key, Claim claim, RateLimitPolicy policy, Throwable failure) {
        LOG.debugf(failure, "Rate limit %s lease claim failed; store-failure-mode applies", policy.id());
        FallbackCost fallbackCost = new FallbackCost(claim);
        cache.asMap().compute(key, fallbackCost);
        run(evicted);
        long cost = fallbackCost.cost;
        if (cost == 0) {
            onReply(key, claim, new Granted(RateLimitDecision.allowed(0), Source.LOCAL_FALLBACK));
            return;
        }
        delegate.fallback(key, cost, policy).subscribe().with(
                new Consumer<>() {
                    @Override
                    public void accept(Granted granted) {
                        onReply(key, claim, granted);
                    }
                },
                new Consumer<>() {
                    @Override
                    public void accept(Throwable error) {
                        LOG.warnf(error, "Rate limit %s failure-mode decision failed", policy.id());
                        onReply(key, claim, new Granted(RateLimitDecision.unavailable(Duration.ZERO),
                                Source.CLOSED_FALLBACK));
                    }
                });
    }

    /**
     * Sets aside the chunk part of a failed claim that its waiters still need, and keeps the cost of the failure-mode
     * decision for the caller. {@code compute} runs it on the calling thread and returns after it, so the caller reads
     * {@link #cost} without synchronization.
     */
    private static final class FallbackCost implements BiFunction<String, Entry, Entry> {

        private final Claim claim;
        long cost;

        FallbackCost(Claim claim) {
            this.claim = claim;
        }

        @Override
        public Entry apply(String key, Entry e) {
            if (e == null || e.inFlight == null || e.inFlight.token != claim.token) {
                // late: only the reserved part can still be settled
                cost = claim.reserved;
                return e;
            }
            e.fallbackChunkPart = e.waiters.isEmpty() ? 0 : claim.chunkPart;
            cost = claim.reserved + e.fallbackChunkPart;
            return e;
        }
    }

    private void onReply(String key, Claim claim, Granted granted) {
        transition(key, new Step() {
            @Override
            public Entry apply(String k, Entry e, List<Runnable> fx) {
                if (e == null || e.inFlight == null || e.inFlight.token != claim.token) {
                    late(claim, granted);
                    return e;
                }
                long now = clock.nanoTime();
                e.inFlight = null;
                LeasingRateLimitStore.this.apply(k, e, claim, granted, now, fx);
                tickAndRearm(k, e, now, fx);
                return e;
            }
        });
    }

    /**
     * The outcome of a claim whose entry was removed, recreated or re-policied: its reserved admissions were moved to
     * {@link Overdraft#UNRECONCILED} and now reach their terminal category, if the outcome tells it.
     */
    private void late(Claim claim, Granted granted) {
        RateLimitDecision decision = granted.decision();
        switch (granted.source()) {
            case STORE -> {
                if (decision instanceof RateLimitDecision.Allowed) {
                    claimsGranted.increment();
                    stranded.add(claim.chunkPart);
                    resolve(Overdraft.SETTLED_LATE, claim.reserved);
                } else {
                    claimsRefused.increment();
                    resolve(Overdraft.FORGIVEN, claim.reserved);
                }
            }
            case LOCAL_FALLBACK -> resolve(
                    decision instanceof RateLimitDecision.Allowed ? Overdraft.SETTLED_FALLBACK : Overdraft.FORGIVEN,
                    claim.reserved);
            // nothing is known about the store's debit: unreconciled is final
            case OPEN_FALLBACK, CLOSED_FALLBACK -> {
            }
        }
    }

    /**
     * A late outcome tells where unreconciled admissions belong: the outstanding figure drops, the entry counter stays.
     */
    private void resolve(Overdraft to, long count) {
        if (count > 0) {
            unreconciledOutstanding.add(-count);
            overdraft.get(to).add(count);
        }
    }

    private void apply(String key, Entry e, Claim claim, Granted granted, long now, List<Runnable> fx) {
        RateLimitDecision decision = granted.decision();
        switch (granted.source()) {
            case STORE -> {
                if (decision instanceof RateLimitDecision.Allowed allowed) {
                    claimsGranted.increment();
                    e.lastRemaining = Math.clamp(allowed.remaining(), 0, e.policy.burst());
                    overdraft.get(Overdraft.SETTLED).add(claim.reserved);
                    if (claim.chunkPart > 0) {
                        long deadline = claim.sendNanos + leaseTime(e.policy);
                        if (deadline - now <= 0) {
                            // the reply arrived after its own deadline: stranded whole
                            stranded.add(claim.chunkPart);
                        } else {
                            dropFallbackChunks(e);
                            e.chunks.add(new Chunk(claim.chunkPart, deadline, Source.STORE, false));
                            e.retainUntilNanos = deadline + leaseTime(e.policy);
                        }
                    }
                    serveWaiters(e, fx);
                } else if (decision instanceof RateLimitDecision.Rejected rejected) {
                    claimsRefused.increment();
                    refused(key, e, claim, rejected, now, fx);
                }
            }
            case LOCAL_FALLBACK -> {
                if (decision instanceof RateLimitDecision.Allowed) {
                    overdraft.get(Overdraft.SETTLED_FALLBACK).add(claim.reserved);
                    fallbackChunk(e, claim, decision, now, fx);
                    serveWaiters(e, fx);
                } else if (decision instanceof RateLimitDecision.Rejected rejected) {
                    // the local bucket stands in for the store: its refusal is handled like one
                    refused(key, e, claim, rejected, now, fx);
                }
            }
            case OPEN_FALLBACK, CLOSED_FALLBACK -> {
                overdraft.get(Overdraft.FORGIVEN).add(claim.reserved);
                answerAll(e, decision, fx);
            }
        }
        if (!e.waiters.isEmpty() && e.inFlight == null) {
            claim(key, e, Kind.ON_DEMAND, now, fx);
        }
    }

    private void refused(String key, Entry e, Claim claim, RateLimitDecision.Rejected rejected, long now,
            List<Runnable> fx) {
        overdraft.get(Overdraft.FORGIVEN).add(claim.reserved);
        long retryAfter = rejected.retryAfter().toNanos();
        if (claim.kind == Kind.PREFETCH) {
            // a private back-off: a refused prefetch says nothing about permits already held
            e.prefetchRefusedUntilNanos = now + retryAfter;
            cooldown(e, now, retryAfter);
            return;
        }
        if (claim.cost() > 1 && !e.waiters.isEmpty()) {
            // a full chunk was refused, a single permit may still be there
            claim(key, e, Kind.ON_DEMAND, 1, now, fx);
            return;
        }
        cooldown(e, now, retryAfter);
        if (claim.cost() == 1) {
            e.refusedUntilNanos = now + retryAfter;
            answerAll(e, rejected, fx);
        }
    }

    /**
     * No optimistic admission before {@code refusalTime + max(T, retryAfter)}.
     */
    private void cooldown(Entry e, long now, long retryAfterNanos) {
        long until = now + Math.max(e.policy.emissionIntervalNanos(), retryAfterNanos);
        if (e.cooldownUntilNanos == NONE || until - e.cooldownUntilNanos > 0) {
            e.cooldownUntilNanos = until;
        }
    }

    /**
     * A fallback grant becomes a short-lived chunk; one whose send-time deadline has already passed still answers the
     * waiters it was claimed for, exactly once, since the local bucket already paid for them.
     */
    private void fallbackChunk(Entry e, Claim claim, RateLimitDecision decision, long now, List<Runnable> fx) {
        long part = e.fallbackChunkPart;
        if (part == 0) {
            return;
        }
        long deadline = claim.sendNanos + Math.min(leaseTime(e.policy), e.policy.emissionIntervalNanos());
        if (deadline - now <= 0) {
            int i = 0;
            Waiter w;
            while (part > 0 && (w = e.waiters.peekFirst()) != null && covered(claim, w, i)) {
                e.waiters.pollFirst();
                answer(w, decision, fx);
                part--;
                i++;
            }
            fallbackDiscarded.add(part);
        } else if (e.heldStore() > 0) {
            // store and fallback chunks are never mixed; the authoritative ones keep being served
            fallbackDiscarded.add(part);
        } else {
            e.chunks.add(new Chunk(part, deadline, Source.LOCAL_FALLBACK, delegate.degraded()));
            long retain = deadline + leaseTime(e.policy);
            if (e.retainUntilNanos == NONE || retain - e.retainUntilNanos > 0) {
                e.retainUntilNanos = retain;
            }
        }
    }

    private void dropFallbackChunks(Entry e) {
        Iterator<Chunk> it = e.chunks.iterator();
        while (it.hasNext()) {
            Chunk c = it.next();
            if (c.source == Source.LOCAL_FALLBACK) {
                it.remove();
                fallbackDiscarded.add(c.left);
            }
        }
    }

    /**
     * As many emission intervals as the key's largest chunk: {@code lease-size}, or the key's burst when an override
     * gives it a smaller one. The key's burst tolerance therefore bounds it, which keeps it from overflowing when an
     * override also gives the key a long emission interval.
     */
    private long leaseTime(RateLimitPolicy policy) {
        return Math.min(leaseSize, policy.burst()) * policy.emissionIntervalNanos();
    }

    // ---- waiters

    private void serveWaiters(Entry e, List<Runnable> fx) {
        Chunk chunk;
        while (!e.waiters.isEmpty() && (chunk = e.chunks.peekFirst()) != null) {
            answer(e.waiters.pollFirst(), serve(e, chunk), fx);
        }
    }

    private void answerAll(Entry e, RateLimitDecision decision, List<Runnable> fx) {
        Waiter w;
        while ((w = e.waiters.pollFirst()) != null) {
            answer(w, decision, fx);
        }
    }

    private static void answer(Waiter w, RateLimitDecision decision, List<Runnable> fx) {
        w.done = true;
        fx.add(new Complete(w.emitter, decision));
    }

    /**
     * A waiter is covered by the in-flight claim when the claim's chunk part reaches its position and the claim's own
     * deadline is no later than the waiter's: that claim's outcome answers it in time, so the timer never does.
     */
    private boolean covered(Claim claim, Waiter w, int position) {
        return claim != null && position < claim.chunkPart
                && w.deadlineNanos - (claim.sendNanos + waiterBudgetNanos) >= 0;
    }

    /**
     * What the entry timer does, also run at the end of every transition: expires uncovered waiters at their deadline
     * with the failure-mode decision and settles optimistic admissions nobody claimed within one emission interval.
     */
    private void tick(String key, Entry e, long now, List<Runnable> fx) {
        int i = 0;
        Iterator<Waiter> it = e.waiters.iterator();
        while (it.hasNext()) {
            Waiter w = it.next();
            // waiters queue in arrival order, so in deadline order: none after the first pending one is due
            if (w.deadlineNanos - now > 0) {
                break;
            }
            if (!covered(e.inFlight, w, i)) {
                it.remove();
                w.done = true;
                fx.add(failureMode(key, e.policy, w.emitter));
                continue;
            }
            i++;
        }
        if (e.unreserved > 0 && e.inFlight == null && now - e.lastAdmissionNanos >= e.policy.emissionIntervalNanos()) {
            claim(key, e, Kind.SETTLE, 0, now, fx);
        }
    }

    private void tickAndRearm(String key, Entry e, long now, List<Runnable> fx) {
        tick(key, e, now, fx);
        rearm(key, e, now);
    }

    private Runnable failureMode(String key, RateLimitPolicy policy, UniEmitter<? super RateLimitDecision> em) {
        return answer(em, delegate.fallback(key, 1, policy).map(new Function<>() {
            @Override
            public RateLimitDecision apply(Granted granted) {
                return granted.decision();
            }
        }));
    }

    /**
     * @return the effect that answers the emitter with the decision, once the transition is over
     */
    private static Runnable answer(UniEmitter<? super RateLimitDecision> em, Uni<RateLimitDecision> decision) {
        return new Runnable() {
            @Override
            public void run() {
                decision.subscribe().with(new Consumer<>() {
                    @Override
                    public void accept(RateLimitDecision d) {
                        em.complete(d);
                    }
                }, new Consumer<>() {
                    @Override
                    public void accept(Throwable failure) {
                        em.fail(failure);
                    }
                });
            }
        };
    }

    private void cancel(String key, Waiter waiter) {
        if (waiter.done) {
            return;
        }
        transition(key, new Step() {
            @Override
            public Entry apply(String k, Entry e, List<Runnable> fx) {
                if (e != null && e.waiters.remove(waiter)) {
                    tickAndRearm(k, e, clock.nanoTime(), fx);
                }
                return e;
            }
        });
    }

    // ---- the entry timer

    /**
     * Arms the entry timer for the first due time, or keeps it when it is already armed for that time: on a hot key, an
     * arrival that does not change it costs neither a timer nor a walk over every waiter.
     */
    private void rearm(String key, Entry e, long now) {
        long due = NONE;
        int i = 0;
        for (Waiter w : e.waiters) {
            // waiters queue in deadline order, and only the first chunk part of them can be covered: the first
            // uncovered one is due first
            if (!covered(e.inFlight, w, i)) {
                due = w.deadlineNanos;
                break;
            }
            i++;
        }
        if (e.unreserved > 0 && e.inFlight == null) {
            long settleAt = e.lastAdmissionNanos + e.policy.emissionIntervalNanos();
            if (due == NONE || settleAt - due < 0) {
                due = settleAt;
            }
        }
        if (due == NONE) {
            cancelTimer(e);
            return;
        }
        if (e.timer != null && e.timerDueNanos == due) {
            return;
        }
        cancelTimer(e);
        e.timerDueNanos = due;
        long serial = e.timerSerial;
        e.timer = clock.schedule(Duration.ofNanos(Math.max(1, due - now)), new Runnable() {
            @Override
            public void run() {
                onTimer(key, serial);
            }
        });
    }

    private void onTimer(String key, long serial) {
        transition(key, new Step() {
            @Override
            public Entry apply(String k, Entry e, List<Runnable> fx) {
                if (e == null || e.timerSerial != serial) {
                    return e;
                }
                e.timer = null;
                tickAndRearm(k, e, clock.nanoTime(), fx);
                return e;
            }
        });
    }

    /**
     * Also makes the serial of every timer scheduled so far stale, so that one that fires despite its cancellation does
     * nothing.
     */
    private static void cancelTimer(Entry e) {
        e.timerSerial++;
        if (e.timer != null) {
            e.timer.cancel();
            e.timer = null;
        }
    }

    // ---- removal

    /**
     * Ends the entry's current life: strands its store chunks, drops fallback ones, answers its waiters with the
     * failure-mode decision, forgives admissions no claim carries and leaves the in-flight claim's reservation
     * unreconciled until its reply, which its token will classify as late. Never calls the store.
     */
    private void retire(String key, Entry e, Collection<Runnable> fx) {
        cancelTimer(e);
        for (Chunk c : e.chunks) {
            (c.source == Source.STORE ? stranded : fallbackDiscarded).add(c.left);
        }
        e.chunks.clear();
        Waiter w;
        while ((w = e.waiters.pollFirst()) != null) {
            w.done = true;
            fx.add(failureMode(key, e.policy, w.emitter));
        }
        overdraft.get(Overdraft.FORGIVEN).add(e.unreserved);
        e.unreserved = 0;
        if (e.inFlight != null) {
            overdraft.get(Overdraft.UNRECONCILED).add(e.inFlight.reserved);
            unreconciledOutstanding.add(e.inFlight.reserved);
            e.inFlight = null;
        }
        e.target = 1;
        e.lastRemaining = NONE;
        e.refusedUntilNanos = NONE;
        e.prefetchRefusedUntilNanos = NONE;
        e.windowStartNanos = NONE;
        e.windowAdmitted = 0;
        e.cooldownUntilNanos = NONE;
        e.retainUntilNanos = NONE;
    }

    /**
     * Waiters get the failure-mode decision, chunks are dropped (store ones counted as stranded).
     */
    @Override
    public void close() {
        for (String key : new ArrayList<>(cache.asMap().keySet())) {
            transition(key, new Step() {
                @Override
                public Entry apply(String k, Entry e, List<Runnable> fx) {
                    if (e != null) {
                        retire(k, e, fx);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Runs one state change of the key inside the compute (phase 1), then the effects the eviction listener collected
     * and those the step collected, in that order (phase 2).
     */
    private void transition(String key, Step step) {
        List<Runnable> fx = new ArrayList<>(2);
        cache.asMap().compute(key, new BiFunction<>() {
            @Override
            public Entry apply(String k, Entry e) {
                return step.apply(k, e, fx);
            }
        });
        run(evicted);
        run(fx);
    }

    @FunctionalInterface
    private interface Step {
        Entry apply(String key, Entry e, List<Runnable> fx);
    }

    private static void run(List<Runnable> effects) {
        for (Runnable effect : effects) {
            effect.run();
        }
    }

    private static void run(Queue<Runnable> effects) {
        Runnable next;
        while ((next = effects.poll()) != null) {
            next.run();
        }
    }

    /**
     * An entry with a claim in flight, waiters or unclaimed admissions never expires by time; otherwise it lives until
     * the latest of one lease time past its last chunk's deadline, its cached refusals, cooldown and overdraft window.
     */
    private static final class EntryExpiry implements Expiry<String, Entry> {

        @Override
        public long expireAfterCreate(String key, Entry e, long currentTime) {
            return expiry(e, currentTime);
        }

        @Override
        public long expireAfterUpdate(String key, Entry e, long currentTime, long currentDuration) {
            return expiry(e, currentTime);
        }

        @Override
        public long expireAfterRead(String key, Entry e, long currentTime, long currentDuration) {
            return expiry(e, currentTime);
        }

        private long expiry(Entry e, long now) {
            if (e.inFlight != null || !e.waiters.isEmpty() || e.unreserved > 0) {
                return Long.MAX_VALUE;
            }
            long duration = 0;
            for (Chunk c : e.chunks) {
                duration = Math.max(duration, c.deadlineNanos - now);
            }
            duration = Math.max(duration, remaining(e.retainUntilNanos, now));
            duration = Math.max(duration, remaining(e.refusedUntilNanos, now));
            duration = Math.max(duration, remaining(e.prefetchRefusedUntilNanos, now));
            duration = Math.max(duration, remaining(e.cooldownUntilNanos, now));
            if (e.windowStartNanos != NONE) {
                duration = Math.max(duration, e.windowStartNanos + e.policy.emissionIntervalNanos() - now);
            }
            return duration;
        }

        private static long remaining(long until, long now) {
            return until == NONE ? 0 : until - now;
        }
    }
}
