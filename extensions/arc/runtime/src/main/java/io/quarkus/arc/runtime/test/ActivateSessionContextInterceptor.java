package io.quarkus.arc.runtime.test;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

// The @ActivateSessionContext interceptor binding is added by the extension
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class ActivateSessionContextInterceptor {

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception {
        ManagedContext sessionContext = Arc.container().sessionContext();
        if (sessionContext.isActive()) {
            return ctx.proceed();
        }
        try {
            sessionContext.activate();
            return ctx.proceed();
        } finally {
            sessionContext.terminate();
        }
    }

}
