package io.quarkus.cache.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import io.quarkus.cache.runtime.CacheInvalidateAllInterceptorBinding.List;

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Repeatable(List.class)
public @interface CacheInvalidateAllInterceptorBinding {

    @Nonbinding
    String cacheName() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        CacheInvalidateAllInterceptorBinding[] value();
    }
}
