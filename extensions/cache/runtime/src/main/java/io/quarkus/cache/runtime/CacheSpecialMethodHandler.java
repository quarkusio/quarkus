package io.quarkus.cache.runtime;

import java.lang.reflect.Method;

import jakarta.interceptor.InvocationContext;

import io.quarkus.cache.CacheResult;

/**
 * Contributes special handling for intercepted cache methods that the default interceptors cannot handle
 * (for example Kotlin {@code suspend} functions).
 * <p>
 * Implementations are registered through {@link CacheRecorder} by companion extensions so that
 * {@code quarkus-cache} stays free of optional language or library dependencies.
 */
public interface CacheSpecialMethodHandler {

    /**
     * Returns {@code true} if this handler can intercept the given method.
     */
    boolean canHandle(Method method);

    /**
     * Handles {@link CacheResult} interception for a method this handler supports.
     */
    Object handleCacheResult(InvocationContext invocationContext, AbstractCache cache, Object key, CacheResult binding)
            throws Exception;
}
