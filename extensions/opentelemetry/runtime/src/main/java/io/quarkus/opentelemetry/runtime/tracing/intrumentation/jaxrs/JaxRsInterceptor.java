package io.quarkus.opentelemetry.runtime.tracing.intrumentation.jaxrs;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.quarkus.arc.ArcInvocationContext;

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
        Class<?> classOfTarget = method.getDeclaringClass();
        Span localRootSpan = LocalRootSpan.current();
        if (logToConsole) {
            System.out.println(
                    "JaxRsInterceptor. intercepted method=" + method.getName() + ", on class=" + classOfTarget.getName()
                            + ", localRootSpan=" + localRootSpan);
        }

        localRootSpan.setAttribute(SemanticAttributes.CODE_NAMESPACE, classOfTarget.getName());
        localRootSpan.setAttribute(SemanticAttributes.CODE_FUNCTION, method.getName());

        return invocationContext.proceed();
    }

    private static boolean evalIsLogToConsole() {
        String envVal = System.getenv("OTEL_LOG_TO_CONSOLE");
        boolean retVal = !("false".equalsIgnoreCase(envVal));
        return retVal;
    }
}
