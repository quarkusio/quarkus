package io.quarkus.arc.test.transform.injectionPoint;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
