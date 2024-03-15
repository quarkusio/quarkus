package io.quarkus.arc.test.interceptors.arcInvContext;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.quarkus.arc.ArcInvocationContext;

@Priority(1)
@Interceptor
@SomeBinding
/**
 * Uses {@link ArcInvocationContext} instead of the classic one.
 */
public class ArcContextInterceptor {

    @AroundInvoke
    Object aroundInvoke(ArcInvocationContext ctx) throws Exception {
        // just to test that bindings are accessible
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        if (ctx.getInterceptorBinding(SomeBinding.class) == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        if (ctx.getInterceptorBindings(SomeBinding.class).isEmpty()) {
            throw new IllegalArgumentException("No bindings found");
        }
        return "" + ctx.proceed() + ArcContextInterceptor.class.getSimpleName();
    }
}
