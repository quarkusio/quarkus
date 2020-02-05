package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.interceptor.Interceptor.Priority;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.runtime.InterceptorBindings;

public abstract class CacheInterceptor {

    public static final int BASE_PRIORITY = Priority.PLATFORM_BEFORE;

    @Inject
    CacheRepository cacheRepository;

    @SuppressWarnings("unchecked")
    protected <T> List<T> getInterceptorBindings(InvocationContext context, Class<T> bindingClass) {
        List<T> bindings = new ArrayList<>();
        for (Annotation binding : InterceptorBindings.getInterceptorBindings(context)) {
            if (bindingClass.isInstance(binding)) {
                bindings.add((T) binding);
            }
        }
        return bindings;
    }

    protected <T> T getInterceptorBinding(InvocationContext context, Class<T> bindingClass) {
        return getInterceptorBindings(context, bindingClass).get(0);
    }

    protected Object buildCacheKey(String cacheName, short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
        // If the method doesn't have any parameter, then a unique default key is generated and used.
        if (methodParameterValues.length == 0) {
            return CacheKeyBuilder.buildDefault(cacheName);
        } else {
            List<Object> keyElements = new ArrayList<>();
            // If at least one of the method parameters is annotated with @CacheKey, then the key is composed of all
            // @CacheKey-annotated parameters that were identified at build time.
            if (cacheKeyParameterPositions.length > 0) {
                for (int i = 0; i < cacheKeyParameterPositions.length; i++) {
                    keyElements.add(methodParameterValues[i]);
                }
            } else {
                // Otherwise, the key is composed of all of the method parameters.
                keyElements.addAll(Arrays.asList(methodParameterValues));
            }
            return CacheKeyBuilder.build(keyElements);
        }
    }
}
