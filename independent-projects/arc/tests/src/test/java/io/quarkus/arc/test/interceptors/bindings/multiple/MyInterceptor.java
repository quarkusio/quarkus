package io.quarkus.arc.test.interceptors.bindings.multiple;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@BarBinding
@FooBinding
public class MyInterceptor {

    public static int TIMES_INVOKED = 0;

    @AroundInvoke
    public Object doAround(InvocationContext context) throws Exception {
        TIMES_INVOKED++;
        return context.proceed();
    }
}
