package io.quarkus.it.amazon.lambda;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Priority(1)
@Count
@Interceptor
public class CounterInterceptor {

    static final AtomicInteger COUNTER = new AtomicInteger();

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        COUNTER.incrementAndGet();
        return context.proceed();
    }

}
