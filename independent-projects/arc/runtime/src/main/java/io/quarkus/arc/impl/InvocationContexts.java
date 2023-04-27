package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.interceptor.InvocationContext;

public final class InvocationContexts {

    private InvocationContexts() {
    }

    /**
     *
     * @param target
     * @param args
     * @param metadata
     * @return the return value
     * @throws Exception
     */
    public static Object performAroundInvoke(Object target, Object[] args, InterceptedMethodMetadata metadata)
            throws Exception {
        return AroundInvokeInvocationContext.perform(target, args, metadata);
    }

    /**
     *
     * @param delegate
     * @param aroundInvokeMethods
     * @param aroundInvokeForward
     * @return the return value
     * @throws Exception
     */
    public static Object performTargetAroundInvoke(InvocationContext delegate,
            List<BiFunction<Object, InvocationContext, Object>> aroundInvokeMethods,
            BiFunction<Object, InvocationContext, Object> aroundInvokeForward) throws Exception {
        return TargetAroundInvokeInvocationContext.perform(delegate, aroundInvokeMethods, aroundInvokeForward);
    }

    /**
     *
     * @param target
     * @param chain
     * @param interceptorBindings
     * @param forward
     * @return a new invocation context
     */
    public static InvocationContext postConstruct(Object target, List<InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings, Runnable forward) {
        return new PostConstructPreDestroyInvocationContext(target, null, interceptorBindings, chain, forward);
    }

    /**
     *
     * @param target
     * @param chain
     * @param interceptorBindings
     * @param forward
     * @return a new invocation context
     */
    public static InvocationContext preDestroy(Object target, List<InterceptorInvocation> chain,
            Set<Annotation> interceptorBindings, Runnable forward) {
        return new PostConstructPreDestroyInvocationContext(target, null, interceptorBindings, chain, forward);
    }

    /**
     *
     * @param target
     * @param chain
     * @param interceptorBindings
     * @return a new {@link jakarta.interceptor.AroundConstruct} invocation context
     */
    public static InvocationContext aroundConstruct(Constructor<?> constructor,
            Object[] parameters,
            List<InterceptorInvocation> chain,
            Function<Object[], Object> forward,
            Set<Annotation> interceptorBindings) {
        return new AroundConstructInvocationContext(constructor, parameters, interceptorBindings, chain, forward);
    }

    /**
     *
     * @param delegate
     * @param methods
     * @param interceptorInstance
     * @return the return value
     * @throws Exception
     */
    public static Object performSuperclassInterception(InvocationContext delegate,
            List<BiFunction<Object, InvocationContext, Object>> methods, Object interceptorInstance, Object[] parameters)
            throws Exception {
        return SuperclassInvocationContext.perform(delegate, methods, interceptorInstance, parameters);
    }

}
