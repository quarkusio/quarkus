package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.util.TypeLiteral;

/**
 * Creational context for synthetic beans.
 *
 * @see BeanCreator
 */
public interface SyntheticCreationalContext<T> extends CreationalContext<T> {

    /**
     *
     * @return the build-time parameters
     */
    Map<String, Object> getParams();

    /**
     * Obtains a contextual reference for a synthetic injection point.
     *
     * @param <R>
     * @param requiredType
     * @param qualifiers
     * @return a contextual reference for the given required type and qualifiers
     * @throws IllegalArgumentException If a corresponding synthetic injection point was not defined
     */
    <R> R getInjectedReference(Class<R> requiredType, Annotation... qualifiers);

    /**
     * Obtains a contextual reference for a synthetic injection point.
     *
     * @param <R>
     * @param requiredType
     * @param qualifiers
     * @return a contextual reference for the given required type and qualifiers
     * @throws IllegalArgumentException If a corresponding synthetic injection point was not defined
     */
    <R> R getInjectedReference(TypeLiteral<R> requiredType, Annotation... qualifiers);

}