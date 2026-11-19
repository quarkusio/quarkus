package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.smallrye.mutiny.Uni;

/**
 * Answers a key that the store itself refused, locally, until the {@code Retry-After} of that refusal has passed: with GCRA, no
 * request of the key can be admitted before then, whichever replica sends it. A client over its limit therefore costs
 * no store call, and its flood can neither saturate the guard that every rule on the store shares nor open its circuit
 * breaker. Used for the rules of a non-local store without {@code lease-size}; lease mode caches its refusals itself.
 * <p>
 * Only single permits are answered from the cache, and only under the policy that was refused, so that a limit resolver
 * that gives the key another limit is still answered by the store. One bounded cache per rule, of
 * {@link DefaultRateLimitPolicy#maxKeys()} keys; a refusal dropped early only costs one more store call.
 */
final class RefusalCachingRateLimitStore implements RateLimitStore {

    private record Refusal(long untilNanos, RateLimitPolicy policy) {
    }

    private final GuardedRateLimitStore delegate;
    private final RateLimitClock clock;
    private final Cache<String, Refusal> refusals;

    /**
     * @param maxKeys the maximum number of cached refusals
     * @param customizer applies the {@code RateLimitLocalStoreCustomizer}s to the cache
     */
    RefusalCachingRateLimitStore(GuardedRateLimitStore delegate, long maxKeys,
            RateLimitClock clock, Consumer<Caffeine<Object, Object>> customizer) {
        this.delegate = delegate;
        this.clock = clock;
        this.refusals = KeyCaches.build(maxKeys, new Expiry<>() {
            @Override
            public long expireAfterCreate(String key, Refusal refusal, long currentTime) {
                return Math.max(0, refusal.untilNanos - currentTime);
            }

            @Override
            public long expireAfterUpdate(String key, Refusal refusal, long currentTime, long currentDuration) {
                return Math.max(0, refusal.untilNanos - currentTime);
            }

            @Override
            public long expireAfterRead(String key, Refusal refusal, long currentTime, long currentDuration) {
                return currentDuration;
            }
        }, new RemovalListener<>() {
            @Override
            public void onRemoval(String key, Refusal refusal, RemovalCause cause) {
            }
        }, clock, customizer);
    }

    @Override
    public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
        if (cost != 1) {
            return delegate.tryConsume(key, cost, policy);
        }
        Refusal refusal = refusals.getIfPresent(key);
        if (refusal != null && refusal.policy.equals(policy)) {
            long left = refusal.untilNanos - clock.nanoTime();
            if (left > 0) {
                return Uni.createFrom().item(RateLimitDecision.rejected(Duration.ofNanos(left)));
            }
        }
        return delegate.grant(key, cost, policy)
                .map(new Function<>() {
                    @Override
                    public RateLimitDecision apply(GuardedRateLimitStore.Granted granted) {
                        // a refusal of the local fallback reflects this replica's bucket only, not the store's
                        if (granted.source() == GuardedRateLimitStore.Source.STORE
                                && granted.decision() instanceof RateLimitDecision.Rejected rejected) {
                            refusals.put(key, new Refusal(clock.nanoTime() + rejected.retryAfter().toNanos(), policy));
                        }
                        return granted.decisionOrThrow();
                    }
                });
    }
}
