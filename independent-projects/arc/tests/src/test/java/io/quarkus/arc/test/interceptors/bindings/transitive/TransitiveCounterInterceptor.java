package io.quarkus.arc.test.interceptors.bindings.transitive;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(2)
@CounterBinding
@AnotherAnnotation // this is a transitive binding which brings in CounterBinding + SomeBinding as well

public class TransitiveCounterInterceptor {

    public static Integer timesInvoked = 0;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        // it should effectively interrupt all that CounterInterceptor does
        timesInvoked++;
        return context.proceed();
    }
}
