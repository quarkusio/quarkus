package io.quarkus.opentelemetry.runtime.tracing.cdi;

import static io.quarkus.opentelemetry.runtime.config.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.util.SpanNames;
import io.quarkus.arc.ArcInvocationContext;

@SuppressWarnings("CdiInterceptorInspection")
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class WithSpanInterceptor {
    private final Instrumenter<MethodRequest, Void> instrumenter;

    public WithSpanInterceptor(final OpenTelemetry openTelemetry) {
        InstrumenterBuilder<MethodRequest, Void> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new MethodRequestSpanNameExtractor());

        MethodSpanAttributesExtractor<MethodRequest, Void> attributesExtractor = MethodSpanAttributesExtractor.newInstance(
                MethodRequest::getMethod,
                new WithSpanParameterAttributeNamesExtractor(),
                MethodRequest::getArgs);

        this.instrumenter = builder.addAttributesExtractor(attributesExtractor)
                .buildInstrumenter(methodRequest -> spanKindFromMethod(methodRequest.getAnnotationBindings()));
    }

    @AroundInvoke
    public Object span(final ArcInvocationContext invocationContext) throws Exception {
        MethodRequest methodRequest = new MethodRequest(
                invocationContext.getMethod(),
                invocationContext.getParameters(),
                invocationContext.getInterceptorBindings());

        Context parentContext = Context.current();
        Context spanContext = null;
        Scope scope = null;
        boolean shouldStart = instrumenter.shouldStart(parentContext, methodRequest);
        if (shouldStart) {
            spanContext = instrumenter.start(parentContext, methodRequest);
            scope = spanContext.makeCurrent();
        }

        try {
            Object result = invocationContext.proceed();

            if (shouldStart) {
                instrumenter.end(spanContext, methodRequest, null, null);
            }

            return result;
        } finally {
            if (scope != null) {
                scope.close();
            }
        }
    }

    private static SpanKind spanKindFromMethod(Set<Annotation> annotations) {
        SpanKind spanKind = null;
        for (Annotation annotation : annotations) {
            if (annotation instanceof WithSpan) {
                spanKind = ((WithSpan) annotation).kind();
                break;
            }
        }
        if (spanKind == null) {
            return SpanKind.INTERNAL;
        }
        return spanKind;
    }

    private static final class MethodRequestSpanNameExtractor implements SpanNameExtractor<MethodRequest> {
        @Override
        public String extract(final MethodRequest methodRequest) {
            String spanName = null;

            for (Annotation annotation : methodRequest.getAnnotationBindings()) {
                if (annotation instanceof WithSpan) {
                    spanName = ((WithSpan) annotation).value();
                    break;
                }
            }
            if (spanName.isEmpty()) {
                spanName = SpanNames.fromMethod(methodRequest.getMethod());
            }
            return spanName;
        }
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
            String value;
            SpanAttribute spanAttribute = parameter.getDeclaredAnnotation(SpanAttribute.class);
            if (spanAttribute == null) {
                // Needed because SpanAttribute cannot be transformed
                io.opentelemetry.extension.annotations.SpanAttribute legacySpanAttribute = parameter.getDeclaredAnnotation(
                        io.opentelemetry.extension.annotations.SpanAttribute.class);
                if (legacySpanAttribute == null) {
                    return null;
                } else {
                    value = legacySpanAttribute.value();
                }
            } else {
                value = spanAttribute.value();
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
