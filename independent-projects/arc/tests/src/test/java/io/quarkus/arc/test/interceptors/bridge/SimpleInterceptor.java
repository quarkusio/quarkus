package io.quarkus.arc.test.interceptors.bridge;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.test.interceptors.Counter;

@Simple
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @Inject
    Counter counter;

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        counter.incrementAndGet();
        return ctx.proceed();
    }
}
