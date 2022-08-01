package io.quarkus.arc.test.interceptors.inheritance;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@One
@Interceptor
public class Interceptor1 extends OverridenInterceptor {

    @AroundInvoke
    @Override
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed() + "1";
    }

}
