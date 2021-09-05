package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class SuperInterceptor2 {

    @AroundInvoke
    public Object intercept1(InvocationContext ctx) throws Exception {
        return ctx.proceed() + " MUST NOT BE CALLED";
    }

    @AroundConstruct
    public void interceptAroundCtor0(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.aroundConstructVal += SuperInterceptor2.class.getSimpleName();
    }

    @PreDestroy
    public void interceptPreDestroy0(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.preDestroyVal += SuperInterceptor2.class.getSimpleName();
    }

    @PostConstruct
    public void interceptPostCtor0(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.postConstructVal += SuperInterceptor2.class.getSimpleName();
    }
}