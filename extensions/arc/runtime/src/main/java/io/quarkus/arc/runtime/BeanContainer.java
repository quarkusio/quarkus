package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import io.quarkus.arc.ManagedContext;

/**
 * Represents a CDI bean container.
 * <p/>
 * Extensions using this API can also leverage arbitrary methods from running {@link io.quarkus.arc.ArcContainer}
 * which can be obtained by invoking a static method {@link io.quarkus.arc.Arc#container()}.
 */
public interface BeanContainer {

    /**
     * Resolves a bean instance for given bean type and qualifiers.
     * <p/>
     * Performs standard CDI resolution meaning it either returns a bean instance or throws a corresponding exception
     * if the dependency is either unsatisfied or ambiguous.
     *
     * @param beanType type of the bean
     * @param beanQualifiers bean qualifiers
     * @return a bean instance; never {@code null}
     */
    <T> T beanInstance(Class<T> beanType, Annotation... beanQualifiers);

    /**
     * Returns an instance factory for given bean type and qualifiers.
     * <p/>
     * This method performs CDI ambiguous dependency resolution and throws and exception if there are two or more beans
     * with given type and qualifiers.
     * <p/>
     * If no matching bean is found, uses a default fallback factory that will attempt to instantiate a non-CDI object
     * of the given class via no-args constructor.
     * <p/>
     * If you need custom factory behavior, take a look at {@link #beanInstanceFactory(Supplier, Class, Annotation...)}
     *
     * @param type bean type
     * @param qualifiers bean qualifiers
     * @return a bean instance factory, never {@code null}
     */
    <T> Factory<T> beanInstanceFactory(Class<T> type, Annotation... qualifiers);

    /**
     * Returns an instance factory for given bean type and qualifiers.
     * <p/>
     * This method performs CDI ambiguous dependency resolution and throws and exception if there are two or more beans
     * with given type and qualifiers.
     * <p/>
     * If no matching bean is found, delegates all calls to the supplied factory fallback.
     *
     * @param fallbackSupplier supplier to delegate to if there is no bean
     * @param type bean type
     * @param qualifiers bean qualifiers
     * @return a bean instance factory, never {@code null}
     */
    <T> Factory<T> beanInstanceFactory(Supplier<Factory<T>> fallbackSupplier, Class<T> type,
            Annotation... qualifiers);

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
