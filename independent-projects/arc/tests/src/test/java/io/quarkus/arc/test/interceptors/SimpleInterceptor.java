package io.quarkus.arc.test.interceptors;

import io.quarkus.arc.ArcInvocationContext;
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
        Object bindings = ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        if (bindings == null) {
            throw new IllegalArgumentException("No bindings found");
        }
        return "" + counter.get() + ctx.proceed() + counter.incrementAndGet();
    }
}
