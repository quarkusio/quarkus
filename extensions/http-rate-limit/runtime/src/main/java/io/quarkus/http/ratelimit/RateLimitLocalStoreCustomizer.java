package io.quarkus.http.ratelimit;

import io.smallrye.common.annotation.Experimental;

/**
 * An escape hatch that most applications never need: a CDI bean implementing it customizes the caches that hold
 * the in-memory state of a rule, for example to record statistics.
 * <p>
 * Every implementation is called in {@linkplain #priority() priority} order for every cache the extension builds for a
 * rule, after the extension has configured it and before it is built: the cache of the {@code local} store, which is
 * also the in-memory fallback of a store whose failure mode is {@code local}, and the lease cache of a rule with
 * {@code lease-size}. A lease rule may therefore have two caches, and the customizer is called for each. The caches are
 * built at startup.
 * <p>
 * A customizer changes only what {@link Settings} offers. The maximum size ({@code max-keys}), the expiry and the
 * eviction of the caches are part of the limit's correctness and of its memory bound, and stay the extension's; the
 * caches themselves are an implementation detail.
 */
@Experimental("Customizers of the HTTP rate limiter are experimental")
public interface RateLimitLocalStoreCustomizer extends Comparable<RateLimitLocalStoreCustomizer> {

    int MINIMUM_PRIORITY = Integer.MIN_VALUE;
    int MAXIMUM_PRIORITY = Integer.MAX_VALUE;
    int DEFAULT_PRIORITY = 0;

    /**
     * @param rule the name of the rule the cache belongs to
     * @param settings what a customizer may change in the cache, before it is built
     */
    void customize(String rule, Settings settings);

    /**
     * Defines the order in which the customizers are applied: a customizer with a lower priority is applied after one
     * with a higher priority, and so has the last word.
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    default int compareTo(RateLimitLocalStoreCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }

    /**
     * What a customizer may change in the in-memory cache of a rule.
     */
    interface Settings {

        /**
         * Reports the activity of the cache to {@code statistics}. Several customizers may each register their own.
         *
         * @return these settings
         */
        Settings statistics(Statistics statistics);
    }

    /**
     * Receives the activity of the in-memory cache of a rule. It is called on the thread that uses the cache, usually an
     * event loop, so it must be fast and must not block, as a counter is.
     */
    interface Statistics {

        /**
         * @param count lookups that found the state of a key
         */
        default void hits(int count) {
        }

        /**
         * @param count lookups that found no state for the key
         */
        default void misses(int count) {
        }

        /**
         * A decision read and wrote the state of a key.
         */
        default void updated() {
        }

        /**
         * The state of a key was dropped to keep the cache within {@code max-keys}: the key starts again with a full
         * burst.
         */
        default void evicted() {
        }

        /**
         * The state of a key expired: its bucket was full again, which is the same as no state.
         */
        default void expired() {
        }
    }
}
