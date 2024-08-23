package io.quarkus.cache.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * This interceptor binding is added at build time on a method if:
 * <ul>
 * <li>it is annotated with {@link io.quarkus.cache.CacheResult CacheResult} or {@link io.quarkus.cache.CacheInvalidate
 * CacheInvalidate}</li>
 * <li>at least one of its arguments is annotated with {@link io.quarkus.cache.CacheKey CacheKey}</li>
 * </ul>
 * It helps improving performances by storing at build time the positions of {@link io.quarkus.cache.CacheKey
 * CacheKey}-annotated arguments instead of relying on reflection at run time (which is bad for performances) to identify these
 * positions.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface CacheKeyParameterPositions {

    @Nonbinding
    short[] value() default {};
}
