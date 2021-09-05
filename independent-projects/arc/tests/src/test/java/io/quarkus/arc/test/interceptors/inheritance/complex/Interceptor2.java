package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1002)
@Binding
public class Interceptor2 extends SuperInterceptor2 {

    @AroundInvoke
    public Object intercept1(InvocationContext ctx) throws Exception {
        return ctx.proceed() + Interceptor2.class.getSimpleName();
    }

    @AroundConstruct
    public void interceptAroundCtor1(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.aroundConstructVal += Interceptor2.class.getSimpleName();
    }

    @PreDestroy
    public void interceptPreDestroy1(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.preDestroyVal += Interceptor2.class.getSimpleName();
    }

    @PostConstruct
    public void interceptPostCtor1(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.postConstructVal += Interceptor2.class.getSimpleName();
    }
}
