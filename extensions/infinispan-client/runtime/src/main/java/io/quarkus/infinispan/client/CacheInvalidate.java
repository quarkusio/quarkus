package io.quarkus.infinispan.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import io.quarkus.infinispan.client.CacheInvalidate.List;

/**
 * When a method annotated with {@link CacheInvalidate} is invoked, Quarkus will use the method argument as key to try to
 * remove an existing entry from the Infinispan cache. If the key does not identify any cache entry, nothing will happen.
 * <p>
 * This annotation can be combined with {@link CacheResult} annotation on a single method. Caching operations will always
 * be executed in the same order: {@link CacheInvalidateAll} first, then {@link CacheInvalidate} and finally
 * {@link CacheResult}.
 */
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(List.class)
public @interface CacheInvalidate {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String cacheName();

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        CacheInvalidate[] value();
    }
}
