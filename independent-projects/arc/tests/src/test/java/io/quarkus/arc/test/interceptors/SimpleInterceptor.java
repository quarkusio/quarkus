package io.quarkus.arc.test.interceptors;

import io.quarkus.arc.ArcInvocationContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Simple
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @Inject
    Counter counter;

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        Object bindings = ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        return "" + counter.get() + ctx.proceed() + counter.incrementAndGet();
    }
}
