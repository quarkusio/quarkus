package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class MiddleInterceptor1 extends SuperInterceptor1 {

    @AroundInvoke
    public Object intercept1(InvocationContext ctx) throws Exception {
        return ctx.proceed() + MiddleInterceptor1.class.getSimpleName();
    }
}
