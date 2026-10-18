package io.quarkus.arc.test.interceptors.no.priority;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Simple
@Interceptor
public class SimpleInterceptorWithNoPriority {

    public static boolean INTERCEPTOR_TRIGGERED = false;

    @AroundInvoke
    Object foo(InvocationContext ctx) throws Exception {
        INTERCEPTOR_TRIGGERED = true;
        return ctx.proceed();
    }
}
