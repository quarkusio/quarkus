package io.quarkus.arc.test.interceptors.bindings.transitive;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@CounterBinding
public class CounterInterceptor {

    public static Integer timesInvoked = 0;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        timesInvoked++;
        return context.proceed();
    }
}
