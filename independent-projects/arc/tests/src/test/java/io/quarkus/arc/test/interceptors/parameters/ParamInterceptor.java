package io.quarkus.arc.test.interceptors.parameters;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
