package io.quarkus.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;

import io.quarkus.cache.CacheInvalidate.List;

/**
 * When a method annotated with {@link CacheInvalidate} is invoked, Quarkus will compute a cache key and use it to try to
 * remove an existing entry from the cache. If the method has one or more arguments, the key computation is done from all the
 * method arguments if none of them is annotated with {@link CacheKey}, or all the arguments annotated with {@link CacheKey}
 * otherwise. This annotation can also be used on a method with no arguments, a default key derived from the cache name is
 * generated in that case. If the key does not identify any cache entry, nothing will happen.
 * <p>
 * This annotation can be combined with multiple other caching annotations on a single method. Caching operations will always
 * be executed in the same order: {@link CacheInvalidateAll} first, then {@link CacheInvalidate} and finally
 * {@link CacheResult}.
 * <p>
 * The underlying caching provider can be chosen and configured in the Quarkus {@link application.properties} file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(List.class)
public @interface CacheInvalidate {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String cacheName();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        CacheInvalidate[] value();
    }
}
