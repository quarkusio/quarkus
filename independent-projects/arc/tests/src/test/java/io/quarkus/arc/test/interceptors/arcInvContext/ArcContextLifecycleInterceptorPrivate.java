package io.quarkus.arc.test.interceptors.arcInvContext;

import io.quarkus.arc.ArcInvocationContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import java.lang.annotation.Annotation;
import java.util.Set;

@Priority(2)
@Interceptor
@SomeBinding
public class ArcContextLifecycleInterceptorPrivate {

    static boolean PRE_DESTROY_INVOKED = false;
    static boolean POST_CONSTRUCT_INVOKED = false;

    @PostConstruct
    private Object postConstruct(ArcInvocationContext ctx) throws Exception {
        // just to test that bindings are accessible
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        POST_CONSTRUCT_INVOKED = true;
        return ctx.proceed();
    }

    @PreDestroy
    private Object preDestroy(ArcInvocationContext ctx) throws Exception {
        // just to test that bindings are accessible
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        PRE_DESTROY_INVOKED = true;
        return ctx.proceed();
    }
}
