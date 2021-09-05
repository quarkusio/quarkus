package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Priority(1001)
@Binding
public class Interceptor1 extends MiddleInterceptor1 {

    @AroundInvoke
    public Object intercept2(InvocationContext ctx) throws Exception {
        return ctx.proceed() + Interceptor1.class.getSimpleName();
    }

    @AroundConstruct
    public void interceptAroundCtor2(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.aroundConstructVal += Interceptor1.class.getSimpleName();
    }

    @PreDestroy
    public void interceptPreDestroy2(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.preDestroyVal += Interceptor1.class.getSimpleName();
    }

    @PostConstruct
    public void interceptPostCtor2(InvocationContext ctx) throws Exception {
        ctx.proceed();
        ComplexAroundInvokeHierarchyTest.postConstructVal += Interceptor1.class.getSimpleName();
    }
}
