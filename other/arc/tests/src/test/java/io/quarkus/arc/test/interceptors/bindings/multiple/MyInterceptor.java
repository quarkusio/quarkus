package io.quarkus.arc.test.interceptors.bindings.multiple;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
