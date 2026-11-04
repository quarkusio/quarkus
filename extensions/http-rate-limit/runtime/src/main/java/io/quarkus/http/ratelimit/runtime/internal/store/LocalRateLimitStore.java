package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.quarkus.arc.All;
import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitLocalStoreCustomizer;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLogger;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.gcra.Gcra;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * The in-memory store: one bounded Caffeine cache of GCRA theoretical arrival times (TATs) per rule.
 * <p>
 * Caches are keyed by {@link RateLimitPolicy#id()}, never by the policy value, so that a per-key resolved limit (different
 * permits, window or burst) finds the same cache and the same TAT, and counts toward the same {@code maxKeys}. An entry
 * expires exactly when its TAT passes, since from then on it is equivalent to no state, whichever policy wrote it.
 * <p>
 * Decisions are made synchronously on the calling thread; the cache's maintenance also runs on it.
 * <p>
 * It only accepts the policies of the extension, {@link DefaultRateLimitPolicy}, whose internal
 * {@link DefaultRateLimitPolicy#maxKeys() maxKeys} bounds each cache.
 */
@Singleton
@Identifier("local")
public class LocalRateLimitStore implements RateLimitStore {

    private static final Logger LOG = Logger.getLogger(LocalRateLimitStore.class);

    private final RateLimitClock clock;
    private final ThrottledLoggers loggers;
    private final List<RateLimitLocalStoreCustomizer> customizers;
    private final ConcurrentMap<String, PolicyCache> caches = new ConcurrentHashMap<>();

    @Inject
    public LocalRateLimitStore(RateLimitClock clock,
            @All List<RateLimitLocalStoreCustomizer> customizers,
            ThrottledLoggers loggers) {
        this.clock = clock;
        this.customizers = customizers.stream().sorted().toList();
        this.loggers = loggers;
    }

    public LocalRateLimitStore(RateLimitClock clock) {
        this.clock = clock;
        this.customizers = List.of();
        this.loggers = new ThrottledLoggers(clock, ThrottledLoggers.DEFAULT_INTERVAL);
    }

    /**
     * Builds the cache of the policy's rule now rather than on its first use, which runs the
     * {@link RateLimitLocalStoreCustomizer}s at startup.
     */
    public void prepare(DefaultRateLimitPolicy policy) {
        prepare(policy.rule(), policy.phase(), policy.maxKeys());
    }

    public void prepare(String rule, RateLimitPhase phase, long maxKeys) {
        cacheOf(rule, phase, maxKeys);
    }

    @Override
    public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
        Gcra.checkCost(cost, policy);
        Cache<String, Long> cache = cacheOf((DefaultRateLimitPolicy) policy);
        long t = policy.emissionIntervalNanos();
        long tau = policy.burstToleranceNanos();
        Gcra.Outcome outcome;
        if (cost == 0) {
            Long tat = cache.getIfPresent(key);
            outcome = Gcra.tryConsume(tat == null ? Gcra.NO_STATE : tat, clock.nanoTime(), 0, t, tau);
        } else {
            Consume consume = new Consume(clock, cost, t, tau);
            cache.asMap().compute(key, consume);
            outcome = consume.outcome;
        }
        RateLimitDecision decision = outcome.allowed()
                ? RateLimitDecision.allowed(outcome.remaining())
                : RateLimitDecision.rejected(Duration.ofNanos(outcome.retryAfterNanos()));
        return Uni.createFrom().item(decision);
    }

    /**
     * @return the estimated number of keys tracked for the policy's rule, 0 if none; expired keys are removed first,
     *         since the cache otherwise only removes them during later activity
     */
    public long trackedKeys(RateLimitPolicy policy) {
        return trackedKeys(policy.id());
    }

    public long trackedKeys(String id) {
        PolicyCache pc = caches.get(id);
        if (pc == null) {
            return 0;
        }
        pc.cache.cleanUp();
        return pc.cache.estimatedSize();
    }

    /**
     * @return the number of keys of the policy's rule evicted because {@code maxKeys} was reached
     */
    public long sizeEvictions(RateLimitPolicy policy) {
        PolicyCache pc = caches.get(policy.id());
        return pc == null ? 0 : pc.sizeEvictions.get();
    }

    private Cache<String, Long> cacheOf(DefaultRateLimitPolicy policy) {
        return cacheOf(policy.rule(), policy.phase(), policy.maxKeys());
    }

    private Cache<String, Long> cacheOf(String rule, RateLimitPhase phase, long maxKeys) {
        return caches.computeIfAbsent(rule + "/" + phase.configName(), new Function<>() {
            @Override
            public PolicyCache apply(String id) {
                return new PolicyCache(rule, id, maxKeys);
            }
        }).cache;
    }

    // for tests
    Cache<String, Long> cache(RateLimitPolicy policy) {
        return caches.get(policy.id()).cache;
    }

    private final class PolicyCache {

        final Cache<String, Long> cache;
        final AtomicLong sizeEvictions = new AtomicLong();
        final ThrottledLogger warning = loggers.logger(LOG);

        PolicyCache(String rule, String id, long maxKeys) {
            this.cache = KeyCaches.build(maxKeys, new TatExpiry(), new RemovalListener<>() {
                @Override
                public void onRemoval(String key, Long tat, RemovalCause cause) {
                    if (cause == RemovalCause.SIZE) {
                        sizeEvictions.incrementAndGet();
                        warnSizeEviction(id, maxKeys);
                    }
                }
            }, clock, new Consumer<>() {
                @Override
                public void accept(Caffeine<Object, Object> builder) {
                    KeyCaches.customize(customizers, rule, builder);
                }
            });
        }

        private void warnSizeEviction(String id, long maxKeys) {
            warning.warnf(
                    "Rate limit %s reached max-keys (%d) and evicts tracked keys to admit new ones; an evicted key"
                            + " starts again with a full burst. Raise max-keys unless this is a flood of distinct keys",
                    id,
                    maxKeys);
        }
    }

    /**
     * An entry lives until its TAT passes: {@code max(0, tat - now)}, computed on the difference.
     */
    private static final class TatExpiry implements Expiry<String, Long> {

        @Override
        public long expireAfterCreate(String key, Long tat, long currentTime) {
            return Math.max(0, tat - currentTime);
        }

        @Override
        public long expireAfterUpdate(String key, Long tat, long currentTime, long currentDuration) {
            return Math.max(0, tat - currentTime);
        }

        @Override
        public long expireAfterRead(String key, Long tat, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }

    /**
     * Decides a consuming call atomically for its key, and keeps the outcome for the caller. {@code compute} runs it on
     * the calling thread and returns after it, so the caller reads {@link #outcome} without synchronization.
     * <p>
     * It reads the time under the key's lock: a time read before could be older than that of a call that took the lock
     * first, and a rejection that caps its apparent debt would then rewrite that call's TAT earlier.
     */
    private static final class Consume implements BiFunction<String, Long, Long> {

        private final RateLimitClock clock;
        private final long cost;
        private final long t;
        private final long tau;
        Gcra.Outcome outcome;

        Consume(RateLimitClock clock, long cost, long t, long tau) {
            this.clock = clock;
            this.cost = cost;
            this.t = t;
            this.tau = tau;
        }

        /**
         * @return the new TAT of an allowed call; for a rejection, which needs a TAT in the future (cost <= burst), the
         *         TAT unchanged, or capped when its debt exceeded the burst tolerance
         */
        @Override
        public Long apply(String key, Long tat) {
            outcome = Gcra.tryConsume(tat == null ? Gcra.NO_STATE : tat, clock.nanoTime(), cost, t, tau);
            return outcome.newTat();
        }
    }
}
