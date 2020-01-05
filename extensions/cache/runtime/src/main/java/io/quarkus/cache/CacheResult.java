package io.quarkus.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;

/**
 * When a method annotated with {@link CacheResult} is invoked, Quarkus will compute a cache key and use it to check in the
 * cache whether the method has been already invoked. If the method has one or more arguments, the key computation is done from
 * all the method arguments if none of them is annotated with {@link CacheKey}, or all the arguments annotated with
 * {@link CacheKey} otherwise. This annotation can also be used on a method with no arguments, a default key derived from the
 * cache name is generated in that case. If a value is found in the cache, it is returned and the annotated method is never
 * actually executed. If no value is found, the annotated method is invoked and the returned value is stored in the cache using
 * the computed or generated key.
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
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
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
}
