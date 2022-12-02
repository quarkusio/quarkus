package io.quarkus.arc.test.interceptors.bindings.transitive;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
