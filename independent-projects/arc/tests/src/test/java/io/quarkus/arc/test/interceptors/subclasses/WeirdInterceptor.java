package io.quarkus.arc.test.interceptors.subclasses;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@MyBinding
public class WeirdInterceptor {

    public static int timesInvoked = 0;

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        timesInvoked++;
        return this.toString() + ctx.proceed();
    }
}
