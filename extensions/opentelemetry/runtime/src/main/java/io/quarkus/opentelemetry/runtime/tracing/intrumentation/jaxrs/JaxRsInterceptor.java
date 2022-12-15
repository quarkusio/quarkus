package io.quarkus.opentelemetry.runtime.tracing.intrumentation.jaxrs;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;

import io.quarkus.arc.ArcInvocationContext;

@SuppressWarnings("CdiInterceptorInspection")
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class JaxRsInterceptor {

    private boolean logToConsole = true;

    public JaxRsInterceptor() {
        logToConsole = !System.getenv("OTEL_LOG_TO_CONSOLE").equalsIgnoreCase("false");
        System.out.println("JaxrsInterceptor. logToConsole=" + logToConsole);
    }

    @AroundInvoke
    public Object span(final ArcInvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        if (logToConsole) {
            System.out.println(
                    "JaxrsInterceptor. intercepted method=" + method.getName() + ", on class=" + targetClass.getName());
        }
        return invocationContext.proceed();
    }
}
