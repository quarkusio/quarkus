package io.quarkus.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import io.quarkus.cache.CacheInvalidate.List;
import io.quarkus.cache.runtime.UndefinedCacheKeyGenerator;

/**
 * When a method annotated with {@link CacheInvalidate} is invoked, Quarkus will compute a cache key and use it to try to
 * remove an existing entry from the cache.
 * <p>
 * The cache key is computed using the following logic:
 * <ul>
 * <li>If a {@link CacheKeyGenerator} is specified with this annotation, then it is used to generate the cache key. The
 * {@link CacheKey @CacheKey} annotations that might be present on some of the method arguments are ignored.</li>
 * <li>Otherwise, if the method has no arguments, then the cache key is an instance of {@link DefaultCacheKey} built from the
 * cache name.</li>
 * <li>Otherwise, if the method has exactly one argument, then that argument is the cache key.</li>
 * <li>Otherwise, if the method has multiple arguments but only one annotated with {@link CacheKey @CacheKey}, then that
 * annotated argument is the cache key.</li>
 * <li>Otherwise, if the method has multiple arguments annotated with {@link CacheKey @CacheKey}, then the cache key is an
 * instance of {@link CompositeCacheKey} built from these annotated arguments.</li>
 * <li>Otherwise, the cache key is an instance of {@link CompositeCacheKey} built from all the method arguments.</li>
 * </ul>
 * <p>
 * If the key does not identify any cache entry, nothing will happen.
 * <p>
 * This annotation can be combined with multiple other caching annotations on a single method. Caching operations will always
 * be executed in the same order: {@link CacheInvalidateAll} first, then {@link CacheInvalidate} and finally
 * {@link CacheResult}.
 * <p>
 * The underlying caching provider can be chosen and configured in the Quarkus {@link application.properties} file.
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

    /**
     * The {@link CacheKeyGenerator} implementation to use to generate a cache key.
     */
    @Nonbinding
    Class<? extends CacheKeyGenerator> keyGenerator() default UndefinedCacheKeyGenerator.class;

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        CacheInvalidate[] value();
    }
}
