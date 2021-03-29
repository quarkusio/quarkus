package io.quarkus.it.amazon.lambda;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
