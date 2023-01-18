package io.quarkus.arc.test.interceptors.defaultmethod;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@NextBinding
@Interceptor
@Priority(1)
public class NextInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        return "next:" + context.proceed();
    }
}
