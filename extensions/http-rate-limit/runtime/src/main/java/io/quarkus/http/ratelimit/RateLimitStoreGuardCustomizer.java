package io.quarkus.http.ratelimit;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.faulttolerance.api.Guard;

/**
 * An escape hatch that most applications never need: a CDI bean implementing it tunes the circuit breaker of the
 * fault tolerance guard of each non-local store, for example for a store with unusual failure behaviour.
 * <p>
 * Every implementation is called once per non-local store, in {@linkplain #priority() priority} order, after the
 * extension has configured the breaker and before the guard is built. The guard is shared by every rule on the store.
 * <p>
 * The breaker is the only part of the guard a customizer can change. The store timeout
 * ({@code quarkus.http.rate-limit.store-timeout}, or {@code quarkus.http.rate-limit.redis.timeout} for the
 * {@code redis} store) and the cap on concurrent calls to the store are enforced by the extension itself, not by the
 * guard, and cannot be changed here. The breaker's {@code delay} also sets when the probe that recovers a degraded
 * store without traffic runs (when the delay ends), and the {@code Retry-After} of the {@code 503} when the failure
 * mode of the store is {@code closed}. A callback given to {@code onStateChange} runs after the extension's own;
 * calling {@code done()} is refused, since the extension completes the breaker.
 * <p>
 * While it is closed, the breaker never counts a {@code io.vertx.core.http.ConnectionPoolTooBusyException} (the
 * client's connection pool and its wait queue are full) as a failure; a pool refusal of the trial call of a half-open
 * breaker does count, and reopens it. Exceptions given to {@code skipOn} are skipped in addition to the pool refusal,
 * and a {@code when} predicate is not consulted for it. As in SmallRye Fault Tolerance, {@code when} cannot be combined
 * with {@code skipOn} or {@code failOn}.
 */
@Experimental("Customizers of the HTTP rate limiter are experimental")
public interface RateLimitStoreGuardCustomizer extends Comparable<RateLimitStoreGuardCustomizer> {

    int MINIMUM_PRIORITY = Integer.MIN_VALUE;
    int MAXIMUM_PRIORITY = Integer.MAX_VALUE;
    int DEFAULT_PRIORITY = 0;

    /**
     * @param storeName the name of the store: {@code redis}, or the {@code @Identifier} of a {@code RateLimitStore}
     *        bean
     * @param breaker the circuit breaker of the store's guard, already configured by the extension
     */
    void customize(String storeName, Guard.Builder.CircuitBreakerBuilder breaker);

    /**
     * Defines the order in which the customizers are applied: a customizer with a lower priority is applied after one
     * with a higher priority, and so has the last word.
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    default int compareTo(RateLimitStoreGuardCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
