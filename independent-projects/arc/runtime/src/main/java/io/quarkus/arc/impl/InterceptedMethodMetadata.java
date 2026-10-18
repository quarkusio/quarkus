package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import jakarta.interceptor.InvocationContext;

/**
 * Immutable metadata for a specific intercepted method.
 */
public class InterceptedMethodMetadata {

    public final List<InterceptorInvocation> chain;
    public final Method method;
    public final Set<Annotation> bindings;
    public final BiFunction<Object, InvocationContext, Object> aroundInvokeForward;

    public InterceptedMethodMetadata(List<InterceptorInvocation> chain, Method method, Set<Annotation> bindings,
            BiFunction<Object, InvocationContext, Object> aroundInvokeForward) {
        this.chain = chain;
        this.method = method;
        this.bindings = bindings;
        this.aroundInvokeForward = aroundInvokeForward;
    }

}
