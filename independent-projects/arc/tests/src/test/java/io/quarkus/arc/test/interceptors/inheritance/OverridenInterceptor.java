package io.quarkus.arc.test.interceptors.inheritance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class OverridenInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed() + "overriden";
    }
}
