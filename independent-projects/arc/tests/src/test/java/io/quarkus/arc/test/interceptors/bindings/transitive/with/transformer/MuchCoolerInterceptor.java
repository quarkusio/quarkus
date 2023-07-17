package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@MuchCoolerBinding
public class MuchCoolerInterceptor {

    public static Integer timesInvoked = 0;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        timesInvoked++;
        return context.proceed();
    }
}
