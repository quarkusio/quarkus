package io.quarkus.opentelemetry.runtime.tracing.cdi;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

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
import io.opentelemetry.instrumentation.api.incubator.semconv.util.SpanNames;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;

@SuppressWarnings("CdiInterceptorInspection")
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class WithSpanInterceptor {
    private final Instrumenter<MethodRequest, Void> instrumenter;

    public WithSpanInterceptor(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {
        InstrumenterBuilder<MethodRequest, Void> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new MethodRequestSpanNameExtractor());

        builder.setEnabled(!runtimeConfig.sdkDisabled());

        MethodSpanAttributesExtractor<MethodRequest, Void> attributesExtractor = MethodSpanAttributesExtractor.create(
                MethodRequest::getMethod,
                new WithSpanParameterAttributeNamesExtractor(),
                MethodRequest::getArgs);

        this.instrumenter = builder.addAttributesExtractor(attributesExtractor)
                .buildInstrumenter(new SpanKindExtractor<MethodRequest>() {
                    @Override
                    public SpanKind extract(MethodRequest methodRequest) {
                        return spanKindFromMethod(methodRequest.getAnnotationBindings());
                    }
                });
    }

    @AroundInvoke
    public Object span(final ArcInvocationContext invocationContext) throws Exception {
        MethodRequest methodRequest = new MethodRequest(
                invocationContext.getMethod(),
                invocationContext.getParameters(),
                invocationContext.getInterceptorBindings());

        final Class<?> returnType = invocationContext.getMethod().getReturnType();
        Context parentContext = Context.current();
        boolean shouldStart = instrumenter.shouldStart(parentContext, methodRequest);

        if (!shouldStart) {
            return invocationContext.proceed();
        }

        if (isUni(returnType)) {
            final Context currentSpanContext = instrumenter.start(parentContext, methodRequest);
            final Scope currentScope = currentSpanContext.makeCurrent();
            return ((Uni<Object>) invocationContext.proceed()).onTermination()
                    .invoke(new Functions.TriConsumer<Object, Throwable, Boolean>() {
                        @Override
                        public void accept(Object o, Throwable throwable, Boolean isCancelled) {
                            try {
                                if (isCancelled) {
                                    instrumenter.end(currentSpanContext, methodRequest, null,
                                            new CancellationException());
                                } else if (throwable != null) {
                                    instrumenter.end(currentSpanContext, methodRequest, null, throwable);
                                } else {
                                    instrumenter.end(currentSpanContext, methodRequest, null, null);
                                }
                            } finally {
                                if (currentScope != null) {
                                    currentScope.close();
                                }
                            }
                        }
                    });
        } else if (isMulti(returnType)) {
            final Context currentSpanContext = instrumenter.start(parentContext, methodRequest);
            final Scope currentScope = currentSpanContext.makeCurrent();

            return ((Multi<Object>) invocationContext.proceed()).onTermination().invoke(new BiConsumer<Throwable, Boolean>() {
                @Override
                public void accept(Throwable throwable, Boolean isCancelled) {
                    try {
                        if (isCancelled) {
                            instrumenter.end(currentSpanContext, methodRequest, null, new CancellationException());
                        } else if (throwable != null) {
                            instrumenter.end(currentSpanContext, methodRequest, null, throwable);
                        } else {
                            instrumenter.end(currentSpanContext, methodRequest, null, null);
                        }
                    } finally {
                        if (currentScope != null) {
                            currentScope.close();
                        }
                    }
                }
            });
        } else if (isCompletionStage(returnType)) {
            final Context currentSpanContext = instrumenter.start(parentContext, methodRequest);
            final Scope currentScope = currentSpanContext.makeCurrent();
            return ((CompletionStage<?>) invocationContext.proceed()).whenComplete(new BiConsumer<Object, Throwable>() {
                @Override
                public void accept(Object o, Throwable throwable) {
                    try {
                        if (throwable != null) {
                            instrumenter.end(currentSpanContext, methodRequest, null, throwable);
                        } else {
                            instrumenter.end(currentSpanContext, methodRequest, null, null);
                        }
                    } finally {
                        if (currentScope != null) {
                            currentScope.close();
                        }
                    }
                }
            });
        } else {
            final Context currentSpanContext = instrumenter.start(parentContext, methodRequest);
            final Scope currentScope = currentSpanContext.makeCurrent();
            try {
                Object result = invocationContext.proceed();
                instrumenter.end(currentSpanContext, methodRequest, null, null);
                return result;
            } catch (Throwable t) {
                instrumenter.end(currentSpanContext, methodRequest, null, t);
                throw t;
            } finally {
                if (currentScope != null) {
                    currentScope.close();
                }
            }
        }
    }

    private static boolean isUni(Class<?> clazz) {
        return Uni.class.isAssignableFrom(clazz);
    }

    private static boolean isMulti(Class<?> clazz) {
        return Multi.class.isAssignableFrom(clazz);
    }

    private static boolean isCompletionStage(Class<?> clazz) {
        return CompletionStage.class.isAssignableFrom(clazz);
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
