package io.quarkus.arc.test.interceptors.inheritance;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class OverridenInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed() + "overriden";
    }
}
