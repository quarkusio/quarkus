package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1001)
@Binding
public class Interceptor1 extends MiddleInterceptor1 {

    @AroundInvoke
    public Object intercept2(InvocationContext ctx) throws Exception {
        return ctx.proceed() + Interceptor1.class.getSimpleName();
    }
}
