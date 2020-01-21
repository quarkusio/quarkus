package io.quarkus.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;

/**
 * When a method annotated with {@link CacheInvalidateAll} is invoked, Quarkus will remove all entries from the cache.
 * <p>
 * You can only use one of the cache operations (and this annotation) on a given method: {@link CacheResult},
 * {@link CacheInvalidate} or {@link CacheInvalidateAll}.
 * <p>
 * The underlying caching provider can be chosen and configured in the Quarkus {@link application.properties} file.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheInvalidateAll {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String cacheName();
}
