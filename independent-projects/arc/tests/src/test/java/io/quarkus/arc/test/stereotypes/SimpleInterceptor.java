package io.quarkus.arc.test.stereotypes;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@SimpleBinding
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return "intercepted" + ctx.proceed();
    }
}
