package io.quarkus.arc.test.interceptors.noclassinterceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@MethodLevel
@Interceptor
@Priority(1)
public class MethodLevelInterceptor {
    public static int AROUND_INVOKE_COUNTER = 0;
    public static int AROUND_CONSTRUCT_COUNTER = 0;

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        AROUND_INVOKE_COUNTER++;
        return ctx.proceed();
    }

    @AroundConstruct
    Object aroundConstruct(InvocationContext ctx) throws Exception {
        AROUND_CONSTRUCT_COUNTER++;
        return ctx.proceed();
    }
}
