package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class MiddleInterceptor1 extends SuperInterceptor1 {

    @AroundInvoke
    public Object intercept1(InvocationContext ctx) throws Exception {
        return ctx.proceed() + MiddleInterceptor1.class.getSimpleName();
    }

    @AroundConstruct
    public void interceptAroundCtor1(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.aroundConstructVal += MiddleInterceptor1.class.getSimpleName();
    }

    @PreDestroy
    public void interceptPreDestroy1(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.preDestroyVal += MiddleInterceptor1.class.getSimpleName();
    }

    @PostConstruct
    public void interceptPostCtor1(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.postConstructVal += MiddleInterceptor1.class.getSimpleName();
    }
}
