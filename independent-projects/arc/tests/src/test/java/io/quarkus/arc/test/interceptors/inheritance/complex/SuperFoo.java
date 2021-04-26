package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class SuperFoo {

    @AroundInvoke
    public Object intercept0(InvocationContext ctx) throws Exception {
        return ctx.proceed() + SuperFoo.class.getSimpleName();
    }
}
