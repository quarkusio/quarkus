package io.quarkus.arc.test.interceptors.exceptionhandling;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

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
