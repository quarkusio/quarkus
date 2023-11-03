package io.quarkus.cache;

import java.lang.reflect.Method;

/**
 * Implement this interface to generate a cache key based on the cached method, its parameters or any data available from within
 * the generator. The implementation is injected as a CDI bean if possible or is instantiated using the default constructor
 * otherwise.
 */
public interface CacheKeyGenerator {

    /**
     * Generates a cache key.
     *
     * @param method the cached method
     * @param methodParams the method parameters
     * @return cache key
     */
    Object generate(Method method, Object... methodParams);
}
