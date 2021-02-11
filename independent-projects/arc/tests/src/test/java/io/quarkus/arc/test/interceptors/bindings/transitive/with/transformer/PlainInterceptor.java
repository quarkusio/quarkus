package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1)
@PlainBinding
// @MuchCoolerBinding is added programmatically
public class PlainInterceptor {

    public static Integer timesInvoked = 0;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        timesInvoked++;
        return context.proceed();
    }
}
