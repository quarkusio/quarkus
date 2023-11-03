package io.quarkus.infinispan.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import io.quarkus.infinispan.client.CacheInvalidateAll.List;

/**
 * When a method annotated with {@link CacheInvalidateAll} is invoked, Quarkus will remove all entries from the Infinispan
 * cache.
 * <p>
 * This annotation can be combined with {@link CacheResult} annotation on a single method. Caching operations will always
 * be executed in the same order: {@link CacheInvalidateAll} first, then {@link CacheInvalidate} and finally
 * {@link CacheResult}.
 */
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(List.class)
public @interface CacheInvalidateAll {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String cacheName();

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        CacheInvalidateAll[] value();
    }
}
