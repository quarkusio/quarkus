package io.quarkus.opentelemetry.runtime.tracing.intrumentation.jaxrs;

import io.quarkus.arc.ArcInvocationContext;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import java.lang.reflect.Method;

@JaxRsBinding
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 50)
public class JaxRsInterceptor {

    private boolean logToConsole = true;

    public JaxRsInterceptor() {
        logToConsole = evalIsLogToConsole();
        System.out.println("JaxRsInterceptor. logToConsole=" + logToConsole);
    }

    @AroundInvoke
    public Object span(final ArcInvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        if (logToConsole) {
            System.out.println(
                "JaxRsInterceptor. intercepted method=" + method.getName() + ", on class=" + targetClass.getName());
        }
        return invocationContext.proceed();
    }

    private static boolean evalIsLogToConsole() {
        String envVal = System.getenv("OTEL_LOG_TO_CONSOLE");
        boolean retVal = !("false".equalsIgnoreCase(envVal));
        return retVal;
    }
}
