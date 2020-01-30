package io.quarkus.cache.runtime;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CacheKeyBuilder {

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported by the Quarkus application data cache";

    /**
     * Builds a default immutable and unique cache key from a cache name. This key is intended to be used for no-args methods
     * annotated with {@link io.quarkus.cache.CacheResult CacheResult} or {@link io.quarkus.cache.CacheInvalidate
     * CacheInvalidate}.
     * 
     * @param cacheName cache name
     * @return default immutable and unique cache key
     */
    public static Object buildDefault(String cacheName) {
        return new DefaultCacheKey(cacheName);
    }

    /**
     * Builds a cache key from a list of key elements. The list must be non-null and contain at least one key element.
     * 
     * @param keyElements key elements
     * @return cache key
     */
    public static Object build(List<Object> keyElements) {
        if (keyElements == null || keyElements.isEmpty()) {
            throw new IllegalArgumentException("At least one key element is required to build a cache key");
        } else if (keyElements.size() == 1) {
            if (keyElements.get(0) == null) {
                throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
            }
            return keyElements.get(0);
        } else {
            return new CompositeCacheKey(keyElements);
        }
    }

    private static class DefaultCacheKey {

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

    private static class CompositeCacheKey {

        private final Object[] keyElements;

        public CompositeCacheKey(List<Object> keyElements) {
            this.keyElements = keyElements.toArray(new Object[0]);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(keyElements);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (CompositeCacheKey.class.isInstance(obj)) {
                final CompositeCacheKey other = (CompositeCacheKey) obj;
                return Arrays.deepEquals(keyElements, other.keyElements);
            }
            return false;
        }
    }
}
