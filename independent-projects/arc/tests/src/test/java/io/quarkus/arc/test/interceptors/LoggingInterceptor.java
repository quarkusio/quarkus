package io.quarkus.arc.test.interceptors;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Logging
@Priority(10)
@Interceptor
public class LoggingInterceptor {

    static final AtomicReference<Object> LOG = new AtomicReference<Object>();

    @AroundInvoke
    Object log(InvocationContext ctx) throws Exception {
        Object ret = ctx.proceed();
        LOG.set(ret);
        return ret;
    }
}
