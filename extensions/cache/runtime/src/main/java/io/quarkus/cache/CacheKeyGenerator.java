package io.quarkus.cache;

import java.lang.reflect.Method;

import jakarta.enterprise.context.Dependent;

/**
 * Implement this interface to generate a cache key based on the cached method, its parameters or any data available from within
 * the generator.
 * <p>
 * The class must either represent a CDI bean or declare a public no-args constructor. In case of CDI, there must be exactly one
 * bean that has the class in its set of bean types, otherwise the build fails. The context associated with the scope
 * of the bean must be active when the {@link #generate(Method, Object...)} method is invoked. If the scope is {@link Dependent}
 * then the bean instance is destroyed when the {@link #generate(Method, Object...)} method completes.
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
