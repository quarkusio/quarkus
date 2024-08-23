package io.quarkus.arc.test.interceptors.intercepted;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

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
