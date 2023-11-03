package io.quarkus.arc.test.transform.injectionPoint;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Simple
@Priority(1)
@Interceptor
public class SomeInterceptor {

    @Inject
    private DummyBean bean;

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return ctx.proceed() + bean.generateString();
    }
}
