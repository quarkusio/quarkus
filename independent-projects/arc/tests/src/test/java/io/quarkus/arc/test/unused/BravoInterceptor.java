package io.quarkus.arc.test.unused;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Alpha
@Priority(1)
@Interceptor
public class BravoInterceptor {

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
