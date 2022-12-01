package io.quarkus.arc.test.unused;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
