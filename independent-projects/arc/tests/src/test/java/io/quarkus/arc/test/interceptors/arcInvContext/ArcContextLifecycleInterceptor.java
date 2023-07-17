package io.quarkus.arc.test.interceptors.arcInvContext;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;

import io.quarkus.arc.ArcInvocationContext;

@Priority(1)
@Interceptor
@SomeBinding
public class ArcContextLifecycleInterceptor {

    static boolean PRE_DESTROY_INVOKED = false;
    static boolean POST_CONSTRUCT_INVOKED = false;

    @PostConstruct
    Object postConstruct(ArcInvocationContext ctx) throws Exception {
        // just to test that bindings are accessible
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        POST_CONSTRUCT_INVOKED = true;
        return ctx.proceed();
    }

    @PreDestroy
    Object preDestroy(ArcInvocationContext ctx) throws Exception {
        // just to test that bindings are accessible
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        PRE_DESTROY_INVOKED = true;
        return ctx.proceed();
    }
}
