package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1002)
@Binding
public class Interceptor2 extends SuperInterceptor2 {

    @AroundInvoke
    public Object intercept1(InvocationContext ctx) throws Exception {
        return ctx.proceed() + Interceptor2.class.getSimpleName();
    }
}
