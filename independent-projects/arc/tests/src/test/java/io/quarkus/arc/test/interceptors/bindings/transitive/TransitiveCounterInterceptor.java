package io.quarkus.arc.test.interceptors.bindings.transitive;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(2)
@SomeAnnotation // this is transitive binding, it also brings in @CounterBinding
public class TransitiveCounterInterceptor {

    public static Integer timesInvoked = 0;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        // it should effectively interrupt all that CounterInterceptor does
        timesInvoked++;
        return context.proceed();
    }
}
