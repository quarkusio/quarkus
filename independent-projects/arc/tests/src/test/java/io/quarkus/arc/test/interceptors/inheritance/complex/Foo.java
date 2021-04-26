package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

@Binding
@ApplicationScoped
public class Foo extends MiddleFoo {

    public String ping() {
        return "pong";
    }

    @AroundInvoke
    public Object intercept2(InvocationContext ctx) throws Exception {
        return ctx.proceed() + Foo.class.getSimpleName();
    }
}
