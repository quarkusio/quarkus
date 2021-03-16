package io.quarkus.cache.runtime;

import java.util.Objects;

/**
 * A default cache key is used by the annotations caching API when a no-args method annotated with
 * {@link io.quarkus.cache.CacheResult CacheResult} or {@link io.quarkus.cache.CacheInvalidate CacheInvalidate} is invoked.
 */
public class DefaultCacheKey {

    private final String cacheName;

    /**
     * Constructor.
     * 
     * @param cacheName cache name
     * @throws NullPointerException if the cache name is {@code null}
     */
    public DefaultCacheKey(String cacheName) {
        this.cacheName = Objects.requireNonNull(cacheName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DefaultCacheKey) {
            DefaultCacheKey other = (DefaultCacheKey) obj;
            return Objects.equals(cacheName, other.cacheName);
        }
        return false;
    }
}
