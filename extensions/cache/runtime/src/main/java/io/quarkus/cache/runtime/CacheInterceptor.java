package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.interceptor.Interceptor.Priority;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheManager;

public abstract class CacheInterceptor {

    public static final int BASE_PRIORITY = Priority.PLATFORM_BEFORE;

    private static final Logger LOGGER = Logger.getLogger(CacheInterceptor.class);
    private static final String PERFORMANCE_WARN_MSG = "Cache key resolution based on reflection calls. Please create a GitHub issue in the Quarkus repository, the maintainers might be able to improve your application performance.";

    @Inject
    CacheManager cacheManager;

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

    protected Object getCacheKey(AbstractCache cache, List<Short> cacheKeyParameterPositions, Object[] methodParameterValues) {
        if (methodParameterValues == null || methodParameterValues.length == 0) {
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
}
