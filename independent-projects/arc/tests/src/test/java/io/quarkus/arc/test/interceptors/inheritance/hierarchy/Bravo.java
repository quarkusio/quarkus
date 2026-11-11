package io.quarkus.arc.test.interceptors.inheritance.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class Bravo extends Charlie {

    @AroundInvoke
    public Object bravoIntercept(InvocationContext ctx) throws Exception {
        return "b/" + ctx.proceed() + "/b";
    }

    // This callback is overridden
    @PostConstruct
    void alphaDummyInit(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("b");
        ctx.proceed();
    }

    @PreDestroy
    void bravoDestroy(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("b");
        ctx.proceed();
    }

    // This callback is overridden
    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("B");
        ctx.proceed();
    }

}
