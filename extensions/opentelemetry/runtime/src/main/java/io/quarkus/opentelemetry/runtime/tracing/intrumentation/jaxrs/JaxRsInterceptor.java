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
@Priority(Interceptor.Priority.LIBRARY_AFTER + 50)
public class JaxRsInterceptor {

    @AroundInvoke
    public Object span(final ArcInvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        Class<?> classOfTarget = method.getDeclaringClass();
        Span localRootSpan = LocalRootSpan.current();

        localRootSpan.setAttribute(SemanticAttributes.CODE_NAMESPACE, classOfTarget.getName());
        localRootSpan.setAttribute(SemanticAttributes.CODE_FUNCTION, method.getName());

        return invocationContext.proceed();
    }
}
