package io.quarkus.opentelemetry.runtime.tracing.cdi;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.util.SpanNames;

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
                .newInstrumenter(methodRequest -> spanKindFromMethod(methodRequest.getMethod()));
    }

    @AroundInvoke
    public Object span(final InvocationContext invocationContext) throws Exception {
        MethodRequest methodRequest = new MethodRequest(invocationContext.getMethod(), invocationContext.getParameters());

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

    private static SpanKind spanKindFromMethod(Method method) {
        WithSpan annotation = method.getDeclaredAnnotation(WithSpan.class);
        if (annotation == null) {
            return SpanKind.INTERNAL;
        }
        return annotation.kind();
    }

    private static final class MethodRequestSpanNameExtractor implements SpanNameExtractor<MethodRequest> {
        @Override
        public String extract(final MethodRequest methodRequest) {
            WithSpan annotation = methodRequest.getMethod().getDeclaredAnnotation(WithSpan.class);
            String spanName = annotation.value();
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
            SpanAttribute spanAttribute = parameter.getDeclaredAnnotation(SpanAttribute.class);
            if (spanAttribute == null) {
                return null;
            }
            String value = spanAttribute.value();
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
