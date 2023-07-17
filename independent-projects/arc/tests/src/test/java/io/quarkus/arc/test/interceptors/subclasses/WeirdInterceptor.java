package io.quarkus.arc.test.interceptors.subclasses;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

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
