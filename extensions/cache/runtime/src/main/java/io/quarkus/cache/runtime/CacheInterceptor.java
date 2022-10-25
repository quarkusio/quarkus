package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor.Priority;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheException;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CompositeCacheKey;
import io.smallrye.mutiny.Uni;

public abstract class CacheInterceptor {

    public static final int BASE_PRIORITY = Priority.PLATFORM_BEFORE;

    private static final Logger LOGGER = Logger.getLogger(CacheInterceptor.class);
    private static final String PERFORMANCE_WARN_MSG = "Cache key resolution based on reflection calls. Please create a GitHub issue in the Quarkus repository, the maintainers might be able to improve your application performance.";
    protected static final String UNHANDLED_ASYNC_RETURN_TYPE_MSG = "Unhandled async return type";

    @Inject
    CacheManager cacheManager;

    @Inject
    Instance<CacheKeyGenerator> keyGenerator;

    /*
     * The interception is almost always managed by Arc in a Quarkus application. In such a case, we want to retrieve the
     * interceptor bindings stored by Arc in the invocation context data (very good performance-wise). But sometimes the
     * interception is managed by another CDI interceptors implementation. It can happen for example while using caching
     * annotations on a MicroProfile REST Client method. In that case, we have no other choice but to rely on reflection (with
     * underlying synchronized blocks which are bad for performances) to retrieve the interceptor bindings.
     */
    protected <T extends Annotation> CacheInterceptionContext<T> getInterceptionContext(InvocationContext invocationContext,
            Class<T> interceptorBindingClass, boolean supportsCacheKey) {
        return getArcCacheInterceptionContext(invocationContext, interceptorBindingClass)
                .orElseGet(new Supplier<CacheInterceptionContext<T>>() {
                    @Override
                    public CacheInterceptionContext<T> get() {
                        return getNonArcCacheInterceptionContext(invocationContext, interceptorBindingClass, supportsCacheKey);
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
        List<Short> cacheKeyParameterPositions = new ArrayList<>();
        for (Annotation binding : bindings) {
            if (binding instanceof CacheKeyParameterPositions) {
                for (short position : ((CacheKeyParameterPositions) binding).value()) {
                    cacheKeyParameterPositions.add(position);
                }
            } else if (interceptorBindingClass.isInstance(binding)) {
                interceptorBindings.add(cast(binding, interceptorBindingClass));
            }
        }
        return Optional.of(new CacheInterceptionContext<>(interceptorBindings, cacheKeyParameterPositions));
    }

    private <T extends Annotation> CacheInterceptionContext<T> getNonArcCacheInterceptionContext(
            InvocationContext invocationContext, Class<T> interceptorBindingClass, boolean supportsCacheKey) {
        LOGGER.trace("Retrieving interceptor bindings using reflection");
        List<T> interceptorBindings = new ArrayList<>();
        List<Short> cacheKeyParameterPositions = new ArrayList<>();
        boolean cacheKeyParameterPositionsFound = false;
        for (Annotation annotation : invocationContext.getMethod().getAnnotations()) {
            if (annotation instanceof CacheKeyParameterPositions) {
                cacheKeyParameterPositionsFound = true;
                for (short position : ((CacheKeyParameterPositions) annotation).value()) {
                    cacheKeyParameterPositions.add(position);
                }
            } else if (interceptorBindingClass.isInstance(annotation)) {
                interceptorBindings.add(cast(annotation, interceptorBindingClass));
            }
        }
        if (supportsCacheKey && !cacheKeyParameterPositionsFound) {
            /*
             * This block is a fallback that should ideally never be executed because of the poor performance of reflection
             * calls. If the following warn message is displayed, then it means that we should update the build time bytecode
             * generation to cover the missing case. See RestClientMethodEnhancer for more details.
             */
            LOGGER.warn(PERFORMANCE_WARN_MSG);
            Parameter[] parameters = invocationContext.getMethod().getParameters();
            for (short i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(CacheKey.class)) {
                    cacheKeyParameterPositions.add(i);
                }
            }
        }
        return new CacheInterceptionContext<>(interceptorBindings, cacheKeyParameterPositions);
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T cast(Annotation annotation, Class<T> interceptorBindingClass) {
        return (T) annotation;
    }

    protected Object getCacheKey(Cache cache, Class<? extends CacheKeyGenerator> keyGeneratorClass,
            List<Short> cacheKeyParameterPositions, Method method, Object[] methodParameterValues) {
        if (keyGeneratorClass != UndefinedCacheKeyGenerator.class) {
            return generateKey(keyGeneratorClass, method, methodParameterValues);
        } else if (methodParameterValues == null || methodParameterValues.length == 0) {
            // If the intercepted method doesn't have any parameter, then the default cache key will be used.
            return cache.getDefaultKey();
        } else if (cacheKeyParameterPositions.size() == 1) {
            // If exactly one @CacheKey-annotated parameter was identified for the intercepted method at build time, then this
            // parameter will be used as the cache key.
            return methodParameterValues[cacheKeyParameterPositions.get(0)];
        } else if (cacheKeyParameterPositions.size() >= 2) {
            // If two or more @CacheKey-annotated parameters were identified for the intercepted method at build time, then a
            // composite cache key built from all these parameters will be used.
            List<Object> keyElements = new ArrayList<>();
            for (short position : cacheKeyParameterPositions) {
                keyElements.add(methodParameterValues[position]);
            }
            return new CompositeCacheKey(keyElements.toArray(new Object[0]));
        } else if (methodParameterValues.length == 1) {
            // If the intercepted method has exactly one parameter, then this parameter will be used as the cache key.
            return methodParameterValues[0];
        } else {
            // If the intercepted method has two or more parameters, then a composite cache key built from all these parameters
            // will be used.
            return new CompositeCacheKey(methodParameterValues);
        }
    }

    private <T extends CacheKeyGenerator> Object generateKey(Class<T> keyGeneratorClass, Method method,
            Object[] methodParameterValues) {
        Instance<T> keyGenInstance = keyGenerator.select(keyGeneratorClass);
        if (keyGenInstance.isResolvable()) {
            LOGGER.tracef("Using cache key generator bean from Arc [class=%s]", keyGeneratorClass.getName());
            T keyGen = keyGenInstance.get();
            try {
                return keyGen.generate(method, methodParameterValues);
            } finally {
                keyGenerator.destroy(keyGen);
            }
        } else {
            try {
                LOGGER.tracef("Creating a new cache key generator instance [class=%s]", keyGeneratorClass.getName());
                return keyGeneratorClass.getConstructor().newInstance().generate(method, methodParameterValues);
            } catch (NoSuchMethodException e) {
                // This should never be thrown because the default constructor availability is checked at build time.
                throw new CacheException("No default constructor found in cache key generator [class="
                        + keyGeneratorClass.getName() + "]", e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new CacheException("Cache key generator instantiation failed", e);
            }
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
