package io.quarkus.arc.test.unused;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Alpha
@Priority(1)
@Interceptor
public class AlphaInterceptor {

    @Inject
    Counter counter;

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return "" + counter.get() + ctx.proceed() + counter.incrementAndGet();
    }
}
