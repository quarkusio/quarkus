package io.quarkus.arc.test.validation;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Simple
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
