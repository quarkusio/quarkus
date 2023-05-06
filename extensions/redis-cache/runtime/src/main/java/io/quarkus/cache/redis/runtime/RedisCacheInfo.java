package io.quarkus.cache.redis.runtime;

import java.time.Duration;
import java.util.Optional;

public class RedisCacheInfo {

    /**
     * The cache name
     */
    public String name;

    /**
     * The timeout for the compute method
     */
    public Duration computeTimeout;

    /**
     * The default time to live of the item stored in the cache
     */
    public Optional<Duration> ttl = Optional.empty();

    /**
     * the key prefix allowing to identify the keys belonging to the cache.
     * If not set, use "cache:$cache-name"
     */
    public String prefix;

    /**
     * The default type of the value stored in the cache.
     */
    public String valueType;

    /**
     * The key type, {@code String} by default.
     */
    public String keyType = String.class.getName();

    /**
     * Whether the access to the cache should be using optimistic locking
     * See <a href="https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set">Redis Optimistic
     * Locking</a> for details.
     */
    public boolean useOptimisticLocking = false;
}
