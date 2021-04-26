package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class SuperInterceptor1 {

    @AroundInvoke
    public Object intercept0(InvocationContext ctx) throws Exception {
        return ctx.proceed() + SuperInterceptor1.class.getSimpleName();
    }
}
