package io.quarkus.cache.runtime;

import java.util.Objects;

public class DefaultCacheKey {

    private final String cacheName;

    public DefaultCacheKey(String cacheName) {
        this.cacheName = cacheName;
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
