package io.quarkus.arc.test.interceptors.no.priority;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
