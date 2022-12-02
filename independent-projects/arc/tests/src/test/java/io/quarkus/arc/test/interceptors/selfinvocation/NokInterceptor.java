package io.quarkus.arc.test.interceptors.selfinvocation;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Nok
@Priority(1)
@Interceptor
public class NokInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        return "NOK";
    }
}
