package io.quarkus.micrometer.runtime;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.arc.ArcInvocationContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;

/**
 * Quarkus declared interceptor responsible for intercepting all methods
 * annotated with the {@link Counted} annotation to record a few counter
 * metrics about their execution status.
 *
 * @see Counted
 */
@Interceptor
@MicrometerCounted
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
public class MicrometerCountedInterceptor {

    public final String RESULT_TAG_FAILURE_VALUE = "failure";
    public final String RESULT_TAG_SUCCESS_VALUE = "success";

    private final MeterRegistry meterRegistry;

    public MicrometerCountedInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Intercept methods annotated with the {@link Counted} annotation and expose a few counters about
     * their execution status. By default, record both failed and successful attempts. If the
     * {@link Counted#recordFailuresOnly()} is set to {@code true}, then record only
     * failed attempts. In case of a failure, tags the counter with the simple name of the thrown
     * exception.
     *
     * <p>
     * When the annotated method returns a {@link CompletionStage} or any of its subclasses,
     * the counters will be incremented only when the {@link CompletionStage} is completed.
     * If completed exceptionally a failure is recorded, otherwise if
     * {@link Counted#recordFailuresOnly()} is set to {@code false}, a success is recorded.
     *
     * @return Whatever the intercepted method returns.
     * @throws Throwable When the intercepted method throws one.
     */
    @AroundInvoke
    @SuppressWarnings("unchecked")
    Object countedMethod(ArcInvocationContext context) throws Exception {
        MicrometerCounted counted = context.findIterceptorBinding(MicrometerCounted.class);
        if (counted == null) {
            return context.proceed();
        }
        Method method = context.getMethod();
        Tags commonTags = getCommonTags(method.getDeclaringClass().getName(), method.getName());

        Class<?> returnType = method.getReturnType();
        if (TypesUtil.isCompletionStage(returnType)) {
            try {
                return ((CompletionStage<?>) context.proceed()).whenComplete(new BiConsumer<Object, Throwable>() {
                    @Override
                    public void accept(Object o, Throwable throwable) {
                        recordCompletionResult(counted, commonTags, throwable);
                    }
                });
            } catch (Throwable e) {
                record(counted, commonTags, e);
            }
        } else if (TypesUtil.isUni(returnType)) {
            try {
                return ((Uni<Object>) context.proceed()).onTermination().invoke(
                        new Functions.TriConsumer<>() {
                            @Override
                            public void accept(Object o, Throwable throwable, Boolean cancelled) {
                                recordCompletionResult(counted, commonTags, throwable);
                            }
                        });
            } catch (Throwable e) {
                record(counted, commonTags, e);
            }
        }

        try {
            Object result = context.proceed();
            if (!counted.recordFailuresOnly()) {
                record(counted, commonTags, null);
            }
            return result;
        } catch (Throwable e) {
            record(counted, commonTags, e);
            throw e;
        }
    }

    private void recordCompletionResult(MicrometerCounted counted, Tags commonTags, Throwable throwable) {
        if (throwable != null) {
            record(counted, commonTags, throwable);
        } else if (!counted.recordFailuresOnly()) {
            record(counted, commonTags, null);
        }
    }

    private void record(MicrometerCounted counted, Tags commonTags, Throwable throwable) {
        Counter.Builder builder = Counter.builder(counted.value())
                .tags(commonTags)
                .tags(counted.extraTags())
                .tag("exception", MicrometerRecorder.getExceptionTag(throwable))
                .tag("result", throwable == null ? RESULT_TAG_SUCCESS_VALUE : RESULT_TAG_FAILURE_VALUE);
        String description = counted.description();
        if (!description.isEmpty()) {
            builder.description(description);
        }
        builder.register(meterRegistry).increment();
    }

    private Tags getCommonTags(String className, String methodName) {
        return Tags.of("class", className, "method", methodName);
    }

}
