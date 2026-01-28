package io.quarkus.infinispan.client.runtime.jfr;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

@InterceptorBinding
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface JfrCacheOperation {

    ExecutionMode executionMode();

    Scope scope();

    enum ExecutionMode {
        SYNC,
        ASYNC
    }

    enum Scope {
        SINGLE,
        MULTI,
        CACHE_WIDE
    }
}
