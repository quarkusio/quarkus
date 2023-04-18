package io.quarkus.arc.test.interceptors.inheritance.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class Charlie {

    @AroundInvoke
    public Object charlieIntercept(InvocationContext ctx) throws Exception {
        return "c/" + ctx.proceed() + "/c";
    }

    @PostConstruct
    void charlieInit(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("c");
        ctx.proceed();
    }

    // This callback is overriden
    @PreDestroy
    void destroy(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("c");
        ctx.proceed();
    }

    @AroundConstruct
    public void charlieConstruct(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("C");
        ctx.proceed();
    }
}
