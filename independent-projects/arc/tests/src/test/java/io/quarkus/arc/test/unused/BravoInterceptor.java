package io.quarkus.arc.test.unused;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Alpha
@Priority(1)
@Interceptor
public class BravoInterceptor {

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
