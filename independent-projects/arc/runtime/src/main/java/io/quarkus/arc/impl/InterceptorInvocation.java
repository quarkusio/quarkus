package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableInterceptor;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;

public final class InterceptorInvocation {

    public static InterceptorInvocation aroundInvoke(InjectableInterceptor<?> interceptor, Object interceptorInstance,
            int callbackIndex) {
        return new InterceptorInvocation(InterceptionType.AROUND_INVOKE, interceptor, interceptorInstance, callbackIndex);
    }

    public static InterceptorInvocation postConstruct(InjectableInterceptor<?> interceptor, Object interceptorInstance,
            int callbackIndex) {
        return new InterceptorInvocation(InterceptionType.POST_CONSTRUCT, interceptor, interceptorInstance, callbackIndex);
    }

    public static InterceptorInvocation preDestroy(InjectableInterceptor<?> interceptor, Object interceptorInstance,
            int callbackIndex) {
        return new InterceptorInvocation(InterceptionType.PRE_DESTROY, interceptor, interceptorInstance, callbackIndex);
    }

    public static InterceptorInvocation aroundConstruct(InjectableInterceptor<?> interceptor, Object interceptorInstance,
            int callbackIndex) {
        return new InterceptorInvocation(InterceptionType.AROUND_CONSTRUCT, interceptor, interceptorInstance, callbackIndex);
    }

    private final InterceptionType interceptionType;

    @SuppressWarnings("rawtypes")
    private final InjectableInterceptor interceptor;

    private final Object interceptorInstance;

    private final int callbackIndex;

    InterceptorInvocation(InterceptionType interceptionType, InjectableInterceptor<?> interceptor,
            Object interceptorInstance, int callbackIndex) {
        this.interceptionType = interceptionType;
        this.interceptor = interceptor;
        this.interceptorInstance = interceptorInstance;
        this.callbackIndex = callbackIndex;
    }

    @SuppressWarnings("unchecked")
    Object invoke(InvocationContext ctx) throws Exception {
        return interceptor.intercept(interceptionType, interceptorInstance, ctx);
    }

    public int getCallbackIndex() {
        return callbackIndex;
    }
}
