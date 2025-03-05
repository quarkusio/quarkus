package io.quarkus.cache.redis.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface RedisCacheRuntimeConfig {
    /**
     * The default time to live of the item stored in the cache.
     *
     * @deprecated Use {@link #expireAfterWrite} instead. This will be removed after Quarkus 3.20.
     */
    @Deprecated(forRemoval = true, since = "3.3.0")
    Optional<Duration> ttl();

    /**
     * Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after
     * the entry's creation, or the most recent replacement of its value.
     */
    Optional<Duration> expireAfterWrite();

    /**
     * Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after
     * the last access of its value.
     */
    Optional<Duration> expireAfterAccess();

    /**
     * The key prefix allowing to identify the keys belonging to the cache.
     * If not set, the value "{@code cache:{cache-name}}" will be used. The variable
     * "{@code {cache-name}}" is resolved from the value set in the cache annotations.
     */
    Optional<String> prefix();

    /**
     * Whether the access to the cache should be using optimistic locking.
     * See <a href="https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set">Redis Optimistic
     * Locking</a> for details.
     * Default is {@code false}.
     */
    Optional<Boolean> useOptimisticLocking();

    /**
     * If set, the {@code SCAN} command (used to implement invalidation) will have
     * the {@code COUNT} argument with given value. If not set (default),
     * no {@code COUNT} argument is present.
     */
    OptionalInt invalidationScanSize();
}
