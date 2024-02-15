package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ArcInvocationContext;

@Interceptor
@Priority(1)
@MuchCoolerBinding
public class MuchCoolerInterceptor {

    public static int timesInvoked = 0;

    public static Set<Annotation> lastBindings = new HashSet<>();

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        timesInvoked++;
        lastBindings = (Set<Annotation>) context.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        return context.proceed();
    }
}
