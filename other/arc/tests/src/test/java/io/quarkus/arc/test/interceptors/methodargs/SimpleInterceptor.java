package io.quarkus.arc.test.interceptors.methodargs;

import io.quarkus.arc.test.interceptors.Counter;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
