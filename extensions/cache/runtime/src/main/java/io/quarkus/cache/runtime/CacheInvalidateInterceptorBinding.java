package io.quarkus.cache.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import io.quarkus.cache.runtime.CacheInvalidateInterceptorBinding.List;

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Repeatable(List.class)
public @interface CacheInvalidateInterceptorBinding {

    @Nonbinding
    String cacheName() default "";

    @Nonbinding
    short[] cacheKeyParameterPositions() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        CacheInvalidateInterceptorBinding[] value();
    }
}
