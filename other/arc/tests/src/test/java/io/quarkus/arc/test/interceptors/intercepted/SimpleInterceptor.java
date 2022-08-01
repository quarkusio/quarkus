package io.quarkus.arc.test.interceptors.intercepted;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Simple
@Priority(1)
@Interceptor
public class SimpleInterceptor {

    @Inject
    @Intercepted
    Bean<?> bean;

    public static String aroundConstructResult = "something";
    public static String postConstructResult = "something";

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        return bean.getBeanClass().getName() + ctx.proceed();
    }

    @AroundConstruct
    void aroundConstruct(InvocationContext ctx) throws Exception {
        aroundConstructResult = bean.getBeanClass().getName();
        ctx.proceed();
    }

    @PostConstruct
    void postConstruct(InvocationContext ctx) throws Exception {
        postConstructResult = bean.getBeanClass().getName();
        ctx.proceed();
    }
}
