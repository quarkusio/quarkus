package io.quarkus.arc.test.interceptors.defaultmethod;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@NextBinding
@Interceptor
@Priority(1)
public class NextInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        return "next:" + context.proceed();
    }
}
