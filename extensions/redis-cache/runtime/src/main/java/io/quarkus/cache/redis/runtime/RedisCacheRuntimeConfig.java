package io.quarkus.cache.redis.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RedisCacheRuntimeConfig {

    /**
     * The timeout for the compute method
     */
    @ConfigItem
    public Optional<Duration> computeTimeout;

    /**
     * The default time to live of the item stored in the cache
     */
    @ConfigItem
    public Optional<Duration> ttl;

    /**
     * the key prefix allowing to identify the keys belonging to the cache.
     * If not set, use "cache:$cache-name"
     */
    @ConfigItem
    public Optional<String> prefix;

    /**
     * Whether the access to the cache should be using optimistic locking.
     * See <a href="https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set">Redis Optimistic
     * Locking</a> for details.
     * Default is {@code false}.
     */
    @ConfigItem
    public Optional<Boolean> useOptimisticLocking;

}
