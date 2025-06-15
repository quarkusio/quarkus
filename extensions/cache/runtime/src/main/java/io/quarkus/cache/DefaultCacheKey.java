package io.quarkus.cache;

import java.util.Objects;

/**
 * A default cache key is used by the annotations caching API when a no-args method annotated with {@link CacheResult}
 * or {@link CacheInvalidate} is invoked. This class can also be used with the programmatic caching API.
 */
public class DefaultCacheKey {

    private final String cacheName;

    /**
     * Constructor.
     *
     * @param cacheName
     *        cache name
     *
     * @throws NullPointerException
     *         if the cache name is {@code null}
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

    @Override
    public String toString() {
        return "DefaultCacheKey[cacheName=" + cacheName + "]";
    }
}
