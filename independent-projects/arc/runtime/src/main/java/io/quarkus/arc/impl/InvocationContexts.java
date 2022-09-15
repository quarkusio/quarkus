package io.quarkus.arc.impl;

import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class InvocationContexts {

    private InvocationContexts() {
    }

    /**
     *
     * @param target
     * @param method
     * @param aroundInvokeForward
     * @param args
     * @param chain
     * @param interceptorBindings
     * @return the return value
     * @throws Exception
     */
    public static Object performAroundInvoke(Object target, Method method,
            Function<InvocationContext, Object> aroundInvokeForward, Object[] args,
            List<InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings) throws Exception {
        return AroundInvokeInvocationContext.perform(target, method, aroundInvokeForward, args, chain, interceptorBindings);
    }

    /**
     *
     * @param target
     * @param chain
     * @param interceptorBindings
     * @return a new invocation context
     */
    public static InvocationContext postConstruct(Object target, List<InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings) {
        return new LifecycleCallbackInvocationContext(target, null, interceptorBindings, chain);
    }

    /**
     *
     * @param target
     * @param chain
     * @param interceptorBindings
     * @return a new invocation context
     */
    public static InvocationContext preDestroy(Object target, List<InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings) {
        return new LifecycleCallbackInvocationContext(target, null, interceptorBindings, chain);
    }

    /**
     *
     * @param target
     * @param chain
     * @param interceptorBindings
     * @return a new {@link jakarta.interceptor.AroundConstruct} invocation context
     */
    public static InvocationContext aroundConstruct(Constructor<?> constructor,
            List<InterceptorInvocation> chain,
            Supplier<Object> aroundConstructForward,
            Set<Annotation> interceptorBindings) {
        return new AroundConstructInvocationContext(constructor, interceptorBindings, chain, aroundConstructForward);
    }

}
