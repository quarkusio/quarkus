package io.quarkus.tck.restclient.cdi;

import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Loggable
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggableInterceptor {

    private static String invocationMethod;
    private static Class<?> invocationClass;
    private static Object result;

    public static String getInvocationMethod() {
        return invocationMethod;
    }

    public static Class<?> getInvocationClass() {
        return invocationClass;
    }

    public static Object getResult() {
        return result;
    }

    public static void reset() {
        invocationClass = null;
        invocationMethod = null;
        result = null;
    }

    @AroundInvoke
    public Object logInvocation(InvocationContext ctx) throws Exception {
        Method m = ctx.getMethod();
        invocationClass = m.getDeclaringClass();
        invocationMethod = m.getName();

        Object returnVal = ctx.proceed();
        result = returnVal;
        return returnVal;
    }
}
