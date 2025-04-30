package io.quarkus.opentelemetry.runtime.tracing.cdi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.AddingSpanAttributes;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.quarkus.arc.ArcInvocationContext;

/**
 * Will capture the arguments annotated with {@link SpanAttribute} on methods annotated with {@link AddingSpanAttributes}.
 * Will not start a Span if one is not already started.
 */
@SuppressWarnings("CdiInterceptorInspection")
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AddingSpanAttributesInterceptor {

    private final WithSpanParameterAttributeNamesExtractor extractor;

    public AddingSpanAttributesInterceptor() {
        extractor = new WithSpanParameterAttributeNamesExtractor();
    }

    @AroundInvoke
    public Object span(final ArcInvocationContext invocationContext) throws Exception {
        String[] extractedParameterNames = extractor.extract(invocationContext.getMethod(),
                invocationContext.getMethod().getParameters());
        Object[] parameterValues = invocationContext.getParameters();

        Span span = Span.current();
        if (span.isRecording()) {
            try (Scope scope = span.makeCurrent()) {
                for (int i = 0; i < extractedParameterNames.length; i++) {
                    if (extractedParameterNames[i] == null || parameterValues[i] == null) {
                        continue;
                    }
                    span.setAttribute(extractedParameterNames[i], parameterValues[i].toString());
                }
            }
        }
        return invocationContext.proceed();
    }

    private static final class WithSpanParameterAttributeNamesExtractor implements ParameterAttributeNamesExtractor {
        @Override
        public String[] extract(final Method method, final Parameter[] parameters) {
            String[] attributeNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                attributeNames[i] = attributeName(parameters[i]);
            }
            return attributeNames;
        }

        private static String attributeName(Parameter parameter) {
            String value = null;
            SpanAttribute spanAttribute = parameter.getDeclaredAnnotation(SpanAttribute.class);
            if (spanAttribute != null) {
                value = spanAttribute.value();
            } else {
                return null;
            }

            if (!value.isEmpty()) {
                return value;
            } else if (parameter.isNamePresent()) {
                return parameter.getName();
            } else {
                return null;
            }
        }
    }
}
