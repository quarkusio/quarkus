package io.quarkus.arc.test.interceptors.arcInvContext;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.quarkus.arc.ArcInvocationContext;

@Priority(2)
@Interceptor
@SomeBinding
/**
 * Uses {@link io.quarkus.arc.ArcInvocationContext} and the method is private
 */
public class ArcContextInterceptorPrivate {

    @AroundInvoke
    private Object aroundInvoke(ArcInvocationContext ctx) throws Exception {
        // just to test that bindings are accessible
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        return "" + ctx.proceed() + ArcContextInterceptorPrivate.class.getSimpleName();
    }
}
