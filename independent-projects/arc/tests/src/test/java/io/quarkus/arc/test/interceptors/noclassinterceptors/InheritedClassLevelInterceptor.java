package io.quarkus.arc.test.interceptors.noclassinterceptors;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@InheritedClassLevel
@Interceptor
@Priority(1)
public class InheritedClassLevelInterceptor {
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
