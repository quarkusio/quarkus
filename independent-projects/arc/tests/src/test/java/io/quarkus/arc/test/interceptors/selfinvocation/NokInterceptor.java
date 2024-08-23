package io.quarkus.arc.test.interceptors.selfinvocation;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Nok
@Priority(1)
@Interceptor
public class NokInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        return "NOK";
    }
}
