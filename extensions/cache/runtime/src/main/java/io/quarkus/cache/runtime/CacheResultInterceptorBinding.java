package io.quarkus.cache.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface CacheResultInterceptorBinding {

    @Nonbinding
    String cacheName() default "";

    @Nonbinding
    short[] cacheKeyParameterPositions() default {};

    @Nonbinding
    long lockTimeout() default 0;
}
