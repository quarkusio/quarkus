package io.quarkus.arc.test.interceptors.inheritance.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Priority(1)
@AlphaBinding
@Interceptor
public class AlphaInterceptor extends Bravo {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return "a/" + ctx.proceed() + "/a";
    }

    @PostConstruct
    void init(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("a");
        ctx.proceed();
    }

    @Override
    @PreDestroy
    void destroy(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("a");
        ctx.proceed();
    }

    @Override
    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        SuperclassInterceptorMethodsTest.LIFECYCLE_CALLBACKS.add("A");
        ctx.proceed();
    }

    // "If an interceptor method is overridden by another method (regardless whether that method is itself an interceptor method), it will not be invoked."
    @Override
    void alphaDummyInit(InvocationContext ctx) throws Exception {
    }

}
