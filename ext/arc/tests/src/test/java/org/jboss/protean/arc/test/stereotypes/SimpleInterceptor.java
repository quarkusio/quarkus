package org.jboss.protean.arc.test.stereotypes;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@SimpleBinding
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @AroundInvoke
    Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
        return "intercepted" + ctx.proceed();
    }
}
