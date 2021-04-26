package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class MiddleFoo extends SuperFoo {

    @AroundInvoke
    public Object intercept1(InvocationContext ctx) throws Exception {
        return ctx.proceed() + MiddleFoo.class.getSimpleName();
    }
}
