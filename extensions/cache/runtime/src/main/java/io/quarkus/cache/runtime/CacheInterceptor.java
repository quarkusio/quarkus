package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.interceptor.Interceptor.Priority;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheManager;

public abstract class CacheInterceptor {

    public static final int BASE_PRIORITY = Priority.PLATFORM_BEFORE;

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
            Class<T> interceptorBindingClass) {
        return getArcCacheInterceptionContext(invocationContext, interceptorBindingClass)
                .orElse(getNonArcCacheInterceptionContext(invocationContext, interceptorBindingClass));
    }

    private <T extends Annotation> Optional<CacheInterceptionContext<T>> getArcCacheInterceptionContext(
            InvocationContext invocationContext, Class<T> interceptorBindingClass) {
        Set<Annotation> bindings = InterceptorBindings.getInterceptorBindings(invocationContext);
        if (bindings == null) {
            // This should only happen when the interception is not managed by Arc.
            return Optional.empty();
        }
        CacheInterceptionContext<T> result = new CacheInterceptionContext<>();
        for (Annotation binding : bindings) {
            if (binding instanceof CacheKeyParameterPositions) {
                short[] cacheKeyParameterPositions = ((CacheKeyParameterPositions) binding).value();
                result.setCacheKeyParameterPositions(cacheKeyParameterPositions);
            } else if (interceptorBindingClass.isInstance(binding)) {
                result.getInterceptorBindings().add(cast(binding, interceptorBindingClass));
            }
        }
        return Optional.of(result);
    }

    private <T extends Annotation> CacheInterceptionContext<T> getNonArcCacheInterceptionContext(
            InvocationContext invocationContext, Class<T> interceptorBindingClass) {
        CacheInterceptionContext<T> result = new CacheInterceptionContext<>();
        for (Annotation annotation : invocationContext.getMethod().getAnnotations()) {
            if (interceptorBindingClass.isInstance(annotation)) {
                result.getInterceptorBindings().add(cast(annotation, interceptorBindingClass));
            }
        }
        Parameter[] parameters = invocationContext.getMethod().getParameters();
        if (parameters.length > 0) {
            List<Short> cacheKeyParameterPositions = new ArrayList<>();
            for (short i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(CacheKey.class)) {
                    cacheKeyParameterPositions.add(i);
                }
            }
            if (!cacheKeyParameterPositions.isEmpty()) {
                short[] cacheKeyParameterPositionsArray = new short[cacheKeyParameterPositions.size()];
                for (int i = 0; i < cacheKeyParameterPositions.size(); i++) {
                    cacheKeyParameterPositionsArray[i] = cacheKeyParameterPositions.get(i);
                }
                result.setCacheKeyParameterPositions(cacheKeyParameterPositionsArray);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T cast(Annotation annotation, Class<T> interceptorBindingClass) {
        return (T) annotation;
    }

    protected Object getCacheKey(AbstractCache cache, short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
        if (methodParameterValues == null || methodParameterValues.length == 0) {
            // If the intercepted method doesn't have any parameter, then the default cache key will be used.
            return cache.getDefaultKey();
        } else if (cacheKeyParameterPositions.length == 1) {
            // If exactly one @CacheKey-annotated parameter was identified for the intercepted method at build time, then this
            // parameter will be used as the cache key.
            return methodParameterValues[cacheKeyParameterPositions[0]];
        } else if (cacheKeyParameterPositions.length >= 2) {
            // If two or more @CacheKey-annotated parameters were identified for the intercepted method at build time, then a
            // composite cache key built from all these parameters will be used.
            List<Object> keyElements = new ArrayList<>();
            for (int i = 0; i < cacheKeyParameterPositions.length; i++) {
                keyElements.add(methodParameterValues[cacheKeyParameterPositions[i]]);
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
