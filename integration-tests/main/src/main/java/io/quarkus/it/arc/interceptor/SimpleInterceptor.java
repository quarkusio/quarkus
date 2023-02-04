package io.quarkus.it.arc.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Simple(name = "")
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
