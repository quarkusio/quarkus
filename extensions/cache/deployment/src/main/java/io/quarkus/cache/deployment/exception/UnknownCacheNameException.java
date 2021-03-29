package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.DotName;

/**
 * This exception is thrown at build time during the validation phase if a field, a constructor or a method parameter annotated
 * with {@link io.quarkus.cache.CacheName @CacheName} is referencing an unknown cache name. A cache name is unknown if it is not
 * used in any {@link io.quarkus.cache.CacheInvalidate @CacheInvalidate},
 * {@link io.quarkus.cache.CacheInvalidateAll @CacheInvalidateAll} or {@link io.quarkus.cache.CacheResult @CacheResult}
 * annotation.
 */
@SuppressWarnings("serial")
public class UnknownCacheNameException extends RuntimeException {

    private final String cacheName;

    public UnknownCacheNameException(DotName className, String cacheName) {
        super("A field or method parameter is annotated with a @CacheName annotation referencing an unknown cache name [class="
                + className + ", cacheName=" + cacheName + "]");
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }
}
