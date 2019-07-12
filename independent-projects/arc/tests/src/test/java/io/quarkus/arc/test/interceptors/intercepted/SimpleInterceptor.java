package io.quarkus.arc.test.interceptors.intercepted;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
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

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        return bean.getBeanClass().getName() + ctx.proceed();
    }
}
