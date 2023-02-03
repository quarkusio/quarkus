package io.quarkus.infinispan.client.runtime.cache;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor.Priority;
import jakarta.interceptor.InvocationContext;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.CacheException;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.smallrye.mutiny.Uni;

public abstract class CacheInterceptor {

    public static final int BASE_PRIORITY = Priority.PLATFORM_BEFORE;
    protected static final String UNHANDLED_ASYNC_RETURN_TYPE_MSG = "Unhandled async return type";

    private static final Logger LOGGER = Logger.getLogger(CacheInterceptor.class);

    @Inject
    RemoteCacheManager cacheManager;

    /*
     * The interception is almost always managed by Arc in a Quarkus application. In such a case, we want to retrieve the
     * interceptor bindings stored by Arc in the invocation context data (very good performance-wise). But sometimes the
     * interception is managed by another CDI interceptors implementation. It can happen for example while using caching
     * annotations on a MicroProfile REST Client method. In that case, we have no other choice but to rely on reflection (with
     * underlying synchronized blocks which are bad for performances) to retrieve the interceptor bindings.
     */
    protected <T extends Annotation> CacheInterceptionContext<T> getInterceptionContext(InvocationContext invocationContext,
            Class<T> interceptorBindingClass) {
        return getArcCacheInterceptionContext(invocationContext, interceptorBindingClass)
                .orElseGet(new Supplier<CacheInterceptionContext<T>>() {
                    @Override
                    public CacheInterceptionContext<T> get() {
                        return getNonArcCacheInterceptionContext(invocationContext, interceptorBindingClass);
                    }
                });
    }

    private <T extends Annotation> Optional<CacheInterceptionContext<T>> getArcCacheInterceptionContext(
            InvocationContext invocationContext, Class<T> interceptorBindingClass) {
        Set<Annotation> bindings = InterceptorBindings.getInterceptorBindings(invocationContext);
        if (bindings == null) {
            LOGGER.trace("Interceptor bindings not found in ArC");
            // This should only happen when the interception is not managed by Arc.
            return Optional.empty();
        }
        List<T> interceptorBindings = new ArrayList<>();
        for (Annotation binding : bindings) {
            if (interceptorBindingClass.isInstance(binding)) {
                interceptorBindings.add((T) binding);
            }
        }
        return Optional.of(new CacheInterceptionContext<>(interceptorBindings));
    }

    private <T extends Annotation> CacheInterceptionContext<T> getNonArcCacheInterceptionContext(
            InvocationContext invocationContext, Class<T> interceptorBindingClass) {
        LOGGER.trace("Retrieving interceptor bindings using reflection");
        List<T> interceptorBindings = new ArrayList<>();
        for (Annotation annotation : invocationContext.getMethod().getAnnotations()) {
            if (interceptorBindingClass.isInstance(annotation)) {
                interceptorBindings.add((T) annotation);
            }
        }
        return new CacheInterceptionContext<>(interceptorBindings);
    }

    protected Object getCacheKey(Object[] methodParameterValues) {
        if (methodParameterValues == null || methodParameterValues.length == 0) {
            // If the intercepted method doesn't have any parameter, raise an exception.
            throw new CacheException("Unable to cache without a key");
        } else if (methodParameterValues.length == 1) {
            // If the intercepted method has exactly one parameter, then this parameter will be used as the cache key.
            return methodParameterValues[0];
        } else {
            // Protobuf type must be used
            return new RuntimeException("A single parameter is needed. Create a Protobuf schema to create a Composite Key.");
        }
    }

    protected static ReturnType determineReturnType(Class<?> returnType) {
        if (Uni.class.isAssignableFrom(returnType)) {
            return ReturnType.Uni;
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return ReturnType.CompletionStage;
        }
        return ReturnType.NonAsync;
    }

    protected Uni<?> asyncInvocationResultToUni(Object invocationResult, ReturnType returnType) {
        if (returnType == ReturnType.Uni) {
            return (Uni<?>) invocationResult;
        } else if (returnType == ReturnType.CompletionStage) {
            return Uni.createFrom().completionStage(new Supplier<>() {
                @Override
                public CompletionStage<?> get() {
                    return (CompletionStage<?>) invocationResult;
                }
            });
        } else {
            throw new CacheException(new IllegalStateException(UNHANDLED_ASYNC_RETURN_TYPE_MSG));
        }
    }

    protected Object createAsyncResult(Uni<Object> cacheValue, ReturnType returnType) {
        if (returnType == ReturnType.Uni) {
            return cacheValue;
        }
        if (returnType == ReturnType.CompletionStage) {
            return cacheValue.subscribeAsCompletionStage();
        }
        throw new CacheException(new IllegalStateException(UNHANDLED_ASYNC_RETURN_TYPE_MSG));
    }

    protected enum ReturnType {
        NonAsync,
        Uni,
        CompletionStage
    }
}
