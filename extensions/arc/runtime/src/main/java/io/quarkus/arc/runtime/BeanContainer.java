package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;

import io.quarkus.arc.ManagedContext;

/**
 * Represents a CDI bean container.
 */
public interface BeanContainer {

    /**
     *
     * @param type
     * @param qualifiers
     * @return a bean instance or {@code null} if no matching bean is found
     */
    default <T> T instance(Class<T> type, Annotation... qualifiers) {
        return instanceFactory(type, qualifiers).create().get();
    }

    /**
     * Note that if there are multiple sub classes of the given type this will return the exact match. This means
     * that this can be used to directly instantiate superclasses of other beans without causing problems. This behavior differs
     * to standard CDI rules where an ambiguous dependency would exist.
     *
     * @param type
     * @param qualifiers
     * @return a bean instance factory, never {@code null}
     */
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
     * @return the context for {@link javax.enterprise.context.RequestScoped}
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
