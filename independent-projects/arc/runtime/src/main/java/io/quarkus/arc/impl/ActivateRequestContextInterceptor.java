package io.quarkus.arc.impl;

import javax.annotation.Priority;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

@Interceptor
@ActivateRequestContext
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class ActivateRequestContextInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return ctx.proceed();
        } else {
            try {
                requestContext.activate();
                return ctx.proceed();
            } finally {
                requestContext.terminate();
            }
        }
    }

}
