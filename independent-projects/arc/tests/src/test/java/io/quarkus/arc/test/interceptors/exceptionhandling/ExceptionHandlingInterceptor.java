package io.quarkus.arc.test.interceptors.exceptionhandling;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@ExceptionHandlingInterceptorBinding
@Priority(1)
@Interceptor
public class ExceptionHandlingInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        if (ctx.getParameters()[0] == ExceptionHandlingCase.OTHER_EXCEPTIONS) {
            throw new MyOtherException();
        }
        return ctx.proceed();
    }
}
