package io.quarkus.arc.test.interceptors.parameters;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Simple
@Priority(1)
@Interceptor
public class ParamInterceptor {

    @AroundInvoke
    Object interceptParameters(InvocationContext ctx) throws Exception {

        Object[] params = ctx.getParameters();
        if (params.length == 1 && params[0] != null) {
            if (params[0] instanceof CharSequence) {
                params[0] = params[0].getClass().getSimpleName();
            } else if (params[0] instanceof Number) {
                params[0] = 123456;
            }
            ctx.setParameters(params);
        }
        return ctx.proceed();
    }
}
