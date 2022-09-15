package io.quarkus.arc.test.interceptors.inheritance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@One
@Interceptor
public class Interceptor1 extends OverridenInterceptor {

    @AroundInvoke
    @Override
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed() + "1";
    }

}
