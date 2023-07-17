package io.quarkus.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import io.quarkus.cache.runtime.UndefinedCacheKeyGenerator;

/**
 * When a method annotated with {@link CacheResult} is invoked, Quarkus will compute a cache key and use it to check in the
 * cache whether the method has been already invoked.
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
 * If a value is found in the cache, it is returned and the annotated method is never actually executed. If no value is found,
 * the annotated method is invoked and the returned value is stored in the cache using the computed key.
 * <p>
 * A method annotated with {@link CacheResult} is protected by a lock on cache miss mechanism. If several concurrent
 * invocations try to retrieve a cache value from the same missing key, the method will only be invoked once. The first
 * concurrent invocation will trigger the method invocation while the subsequent concurrent invocations will wait for the end
 * of the method invocation to get the cached result. The {@code lockTimeout} parameter can be used to interrupt the lock after
 * a given delay. The lock timeout is disabled by default, meaning the lock is never interrupted. See the parameter Javadoc for
 * more details.
 * <p>
 * This annotation cannot be used on a method returning {@code void}. It can be combined with multiple other caching
 * annotations on a single method. Caching operations will always be executed in the same order: {@link CacheInvalidateAll}
 * first, then {@link CacheInvalidate} and finally {@link CacheResult}.
 * <p>
 * The underlying caching provider can be chosen and configured in the Quarkus {@link application.properties} file.
 */
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheResult {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String cacheName();

    /**
     * Delay in milliseconds before the lock on cache miss is interrupted. If such interruption happens, the cached method will
     * be invoked and its result will be returned without being cached. A value of {@code 0} (which is the default one) means
     * that the lock timeout is disabled.
     */
    @Nonbinding
    long lockTimeout() default 0;

    /**
     * The {@link CacheKeyGenerator} implementation to use to generate a cache key.
     */
    @Nonbinding
    Class<? extends CacheKeyGenerator> keyGenerator() default UndefinedCacheKeyGenerator.class;
}
