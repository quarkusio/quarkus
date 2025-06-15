package io.quarkus.cache;

import java.util.Arrays;

/**
 * A composite cache key is used by the annotations caching API when a method annotated with {@link CacheResult} or
 * {@link CacheInvalidate} is invoked and when the cache key is composed of several of the method arguments (annotated
 * with {@link CacheKey} or not). This class can also be used with the programmatic caching API.
 */
public class CompositeCacheKey {

    private final Object[] keyElements;

    /**
     * Constructor.
     *
     * @param keyElements
     *        key elements
     *
     * @throws IllegalArgumentException
     *         if no key elements are provided
     */
    public CompositeCacheKey(Object... keyElements) {
        if (keyElements.length == 0) {
            throw new IllegalArgumentException(
                    "At least one key element is required to create a composite cache key instance");
        }
        this.keyElements = keyElements;
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

    @Override
    public String toString() {
        return "CompositeCacheKey" + Arrays.toString(keyElements);
    }

    public Object[] getKeyElements() {
        return keyElements;
    }
}
