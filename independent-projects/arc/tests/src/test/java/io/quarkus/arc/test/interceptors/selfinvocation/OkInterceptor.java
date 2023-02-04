package io.quarkus.arc.test.interceptors.selfinvocation;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Ok
@Priority(1)
@Interceptor
public class OkInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        return "OK";
    }
}
