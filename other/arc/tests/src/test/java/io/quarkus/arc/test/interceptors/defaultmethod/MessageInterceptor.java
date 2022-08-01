package io.quarkus.arc.test.interceptors.defaultmethod;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@ABinding
@Interceptor
public class MessageInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        return "intercepted:" + context.proceed();
    }
}
