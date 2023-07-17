package io.quarkus.arc.test.interceptors.defaultmethod;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@ABinding
@Interceptor
public class MessageInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        return "intercepted:" + context.proceed();
    }
}
