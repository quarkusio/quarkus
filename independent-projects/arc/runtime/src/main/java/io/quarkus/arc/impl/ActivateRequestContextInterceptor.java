package io.quarkus.arc.impl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.smallrye.mutiny.Uni;
import javax.annotation.Priority;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@ActivateRequestContext
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class ActivateRequestContextInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        if (ctx.getMethod().getReturnType().equals(Uni.class)) {
            return invokeUni(ctx);
        }

        return invoke(ctx);
    }

    private Uni<?> invokeUni(InvocationContext ctx) {
        return Uni.createFrom().deferred(() -> {
            ManagedContext requestContext = Arc.container().requestContext();
            if (requestContext.isActive()) {
                return proceedWithUni(ctx);
            }

            return Uni.createFrom().deferred(() -> {
                requestContext.activate();
                return proceedWithUni(ctx);
            }).eventually(requestContext::terminate);
        });
    }

    private Uni<?> proceedWithUni(InvocationContext ctx) {
        try {
            return (Uni<?>) ctx.proceed();
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

    private Object invoke(InvocationContext ctx) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return ctx.proceed();
        }

        try {
            requestContext.activate();
            return ctx.proceed();
        } finally {
            requestContext.terminate();
        }
    }
}
