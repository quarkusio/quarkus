package io.quarkus.observation.cdi;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.observation.cdi.convention.DefaultObservedInterceptorConvention;
import io.quarkus.observation.cdi.convention.ObservedInterceptorConvention;
import io.quarkus.observation.cdi.convention.ObservedInterceptorDocumentation;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;

// @Observed binding is added at build time by ObservationProcessor.transformObservedAnnotations()
@SuppressWarnings("CdiInterceptorInspection")
@Interceptor
@Priority(PLATFORM_BEFORE)
public class ObservedInterceptor {

    @Inject
    ObservationRegistry registry;

    @Inject
    Instance<ObservedInterceptorConvention> customConventions;

    private final ConcurrentHashMap<Method, DefaultObservedInterceptorConvention> conventionCache = new ConcurrentHashMap<>();

    @AroundInvoke
    public Object observe(ArcInvocationContext ctx) throws Exception {
        final Observed annotation = resolveAnnotation(ctx);
        final Method method = ctx.getMethod();

        // Annotation values are pre-filled at build time by the AnnotationsTransformer
        final DefaultObservedInterceptorConvention defaultConvention = conventionCache.computeIfAbsent(method,
                m -> {
                    //  annotation.name() and annotation.contextualName() set in
                    //  io.quarkus.observation.deployment.ObservationProcessor#transformObservedAnnotations
                    return new DefaultObservedInterceptorConvention(
                            annotation.name(), annotation.contextualName(),
                            m.getDeclaringClass().getName(),
                            m.getName());
                });

        final Observation observation = ObservedInterceptorDocumentation.DEFAULT
                .observation(customConventions.isUnsatisfied() ? null : customConventions.get(),
                        defaultConvention,
                        new Supplier<>() {
                            @Override
                            public ObservedInterceptorContext get() {
                                return new ObservedInterceptorContext(ctx);
                            }
                        },
                        registry);

        // See ObservationProcessor#transformObservedAnnotations for key/pair validations
        String[] kvs = annotation.lowCardinalityKeyValues();
        for (int i = 0; i + 1 < kvs.length; i += 2) {
            observation.lowCardinalityKeyValue(kvs[i], kvs[i + 1]);
        }

        return handleInvocation(observation, ctx);
    }

    @SuppressWarnings("unchecked")
    private Object handleInvocation(Observation observation, ArcInvocationContext ctx)
            throws Exception {
        Class<?> returnType = ctx.getMethod().getReturnType();

        if (Uni.class.isAssignableFrom(returnType)) {
            observation.start();
            Observation.Scope scope = observation.openScope();
            try {
                return ((Uni<Object>) ctx.proceed())
                        .onTermination().invoke(new Functions.TriConsumer<>() {
                            @Override
                            public void accept(Object o, Throwable throwable, Boolean isCancelled) {
                                if (Boolean.TRUE.equals(isCancelled)) {
                                    observation.error(new CancellationException());
                                } else if (throwable != null) {
                                    observation.error(throwable);
                                }
                                scope.close();
                                observation.stop();
                            }
                        });
            } catch (Exception e) {
                observation.error(e);
                scope.close();
                observation.stop();
                throw e;
            }
        }

        if (Multi.class.isAssignableFrom(returnType)) {
            observation.start();
            Observation.Scope scope = observation.openScope();
            try {
                return ((Multi<Object>) ctx.proceed())
                        .onTermination().invoke(new BiConsumer<>() {
                            @Override
                            public void accept(Throwable throwable, Boolean isCancelled) {
                                if (Boolean.TRUE.equals(isCancelled)) {
                                    observation.error(new CancellationException());
                                } else if (throwable != null) {
                                    observation.error(throwable);
                                }
                                scope.close();
                                observation.stop();
                            }
                        });
            } catch (Exception e) {
                observation.error(e);
                scope.close();
                observation.stop();
                throw e;
            }
        }

        if (CompletionStage.class.isAssignableFrom(returnType)) {
            observation.start();
            Observation.Scope scope = observation.openScope();
            try {
                return ((CompletionStage<?>) ctx.proceed()).whenComplete(new BiConsumer<Object, Throwable>() {
                    @Override
                    public void accept(Object o, Throwable throwable) {
                        if (throwable != null) {
                            observation.error(throwable);
                        }
                        scope.close();
                        observation.stop();
                    }
                });
            } catch (Exception e) {
                observation.error(e);
                scope.close();
                observation.stop();
                throw e;
            }
        }

        // Synchronous
        return observation.observeChecked(ctx::proceed);
    }

    private Observed resolveAnnotation(ArcInvocationContext ctx) {
        Set<Annotation> bindings = ctx.getInterceptorBindings();
        for (Annotation binding : bindings) {
            if (binding instanceof Observed) {
                return (Observed) binding;
            }
        }
        throw new IllegalStateException("@Observed binding not found");
    }
}
