package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableInterceptor;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

public final class InterceptorInvocation {

    public static InterceptorInvocation aroundInvoke(InjectableInterceptor<?> interceptor, Object interceptorInstance) {
        return new InterceptorInvocation(InterceptionType.AROUND_INVOKE, interceptor, interceptorInstance);
    }

    public static InterceptorInvocation postConstruct(InjectableInterceptor<?> interceptor, Object interceptorInstance) {
        return new InterceptorInvocation(InterceptionType.POST_CONSTRUCT, interceptor, interceptorInstance);
    }

    public static InterceptorInvocation preDestroy(InjectableInterceptor<?> interceptor, Object interceptorInstance) {
        return new InterceptorInvocation(InterceptionType.PRE_DESTROY, interceptor, interceptorInstance);
    }

    public static InterceptorInvocation aroundConstruct(InjectableInterceptor<?> interceptor, Object interceptorInstance) {
        return new InterceptorInvocation(InterceptionType.AROUND_CONSTRUCT, interceptor, interceptorInstance);
    }

    private final InterceptionType interceptionType;

    @SuppressWarnings("rawtypes")
    private final InjectableInterceptor interceptor;

    private final Object interceptorInstance;

    InterceptorInvocation(InterceptionType interceptionType, InjectableInterceptor<?> interceptor,
            Object interceptorInstance) {
        this.interceptionType = interceptionType;
        this.interceptor = interceptor;
        this.interceptorInstance = interceptorInstance;
    }

    @SuppressWarnings("unchecked")
    Object invoke(InvocationContext ctx) throws Exception {
        return interceptor.intercept(interceptionType, interceptorInstance, ctx);
    }
}
