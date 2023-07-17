package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;

import io.quarkus.arc.ManagedContext;

/**
 * Represents a CDI bean container.
 */
public interface BeanContainer {

    /**
     * Returns a bean instance for given bean type and qualifiers.
     * <p/>
     * This method follows standard CDI rules meaning that if there are two or more eligible beans, an ambiguous
     * dependency exception is thrown.
     * Note that the method is allowed to return {@code null} if there is no matching bean which allows
     * for fallback implementations.
     *
     * @param type
     * @param qualifiers
     * @return a bean instance or {@code null} if no matching bean is found
     */
    default <T> T beanInstance(Class<T> type, Annotation... qualifiers) {
        return beanInstanceFactory(type, qualifiers).create().get();
    }

    /**
     * This method is deprecated and will be removed in future versions.
     * Use {@link #beanInstance(Class, Annotation...)} instead.
     * </p>
     * As opposed to {@link #beanInstance(Class, Annotation...)}, this method does <b>NOT</b> follow CDI
     * resolution rules and in case of ambiguous resolution performs a choice based on the class type parameter.
     *
     * @param type
     * @param qualifiers
     * @return a bean instance or {@code null} if no matching bean is found
     */
    @Deprecated
    default <T> T instance(Class<T> type, Annotation... qualifiers) {
        return instanceFactory(type, qualifiers).create().get();
    }

    /**
     * Returns an instance factory for given bean type and qualifiers.
     * <p/>
     * This method follows standard CDI rules meaning that if there are two or more beans, an ambiguous dependency
     * exception is thrown.
     * Note that the factory itself is still allowed to return {@code null} if there is no matching bean which allows
     * for fallback implementations.
     *
     * @param type
     * @param qualifiers
     * @return a bean instance factory, never {@code null}
     */
    <T> Factory<T> beanInstanceFactory(Class<T> type, Annotation... qualifiers);

    /**
     * This method is deprecated and will be removed in future versions.
     * Use {@link #beanInstanceFactory(Class, Annotation...)} instead.
     * </p>
     * As opposed to {@link #beanInstanceFactory(Class, Annotation...)}, this method does <b>NOT</b> follow CDI
     * resolution rules and in case of ambiguous resolution performs a choice based on the class type parameter.
     *
     * @param type
     * @param qualifiers
     * @return a bean instance factory, never {@code null}
     */
    @Deprecated
    <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers);

    /**
     * <pre>
     * ManagedContext requestContext = beanContainer.requestContext();
     * if (requestContext.isActive()) {
     *     // Perform action
     * } else {
     *     try {
     *         requestContext.activate();
     *         // Perform action
     *     } finally {
     *         requestContext.terminate();
     *     }
     * }
     * </pre>
     *
     * @return the context for {@link jakarta.enterprise.context.RequestScoped}
     * @throws IllegalStateException If the container is not running
     */
    ManagedContext requestContext();

    interface Factory<T> {

        Factory<Object> EMPTY = new Factory<Object>() {
            @Override
            public Instance<Object> create() {
                return null;
            }
        };

        /**
         *
         * @return a bean instance or {@code null} if no matching bean is found
         */
        Instance<T> create();
    }

    interface Instance<T> extends AutoCloseable {

        /**
         *
         * @return the underlying instance
         */
        T get();

        /**
         * releases the underlying instance
         */
        default void close() {
        };
    }
}
