package io.quarkus.arc.test.interceptors;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
