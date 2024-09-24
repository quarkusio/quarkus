package io.quarkus.cache.redis.runtime;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Optional;

public class RedisCacheInfo {

    /**
     * The cache name
     */
    public String name;

    /**
     * The default time to live of the item stored in the cache
     */
    public Optional<Duration> expireAfterAccess = Optional.empty();

    /**
     * The default time to live to add to the item once read
     */
    public Optional<Duration> expireAfterWrite = Optional.empty();

    /**
     * the key prefix allowing to identify the keys belonging to the cache.
     * If not set, use "cache:$cache-name"
     */
    public String prefix;

    /**
     * The default type of the value stored in the cache.
     */
    public Type valueType;

    /**
     * The key type, {@code String} by default.
     */
    public Type keyType = String.class;

    /**
     * Whether the access to the cache should be using optimistic locking
     * See <a href="https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set">Redis Optimistic
     * Locking</a> for details.
     */
    public boolean useOptimisticLocking = false;
}
