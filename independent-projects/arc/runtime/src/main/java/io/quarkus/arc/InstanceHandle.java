package io.quarkus.arc;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.AlterableContext;

/**
 * Represents an instance handle.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InstanceHandle<T> extends AutoCloseable {

    /**
     *
     * @return an instance of {@code T} or {@code null}
     */
    T get();

    /**
     *
     * @return {@code true} if an instance is available, {@code false} otherwise
     */
    default boolean isAvailable() {
        return get() != null;
    }

    /**
     * If an instance is available, returns the value, otherwise returns
     * {@code other}.
     *
     * @param other
     * @return the instance if available, otherwise {@code other}
     */
    default T orElse(T other) {
        T val = get();
        return val != null ? val : other;
    }

    /**
     * Destroy the instance as defined by
     * {@link jakarta.enterprise.context.spi.Contextual#destroy(Object, jakarta.enterprise.context.spi.CreationalContext)}. If
     * this
     * is a CDI contextual instance it is also removed from the underlying context.
     *
     * @see AlterableContext#destroy(jakarta.enterprise.context.spi.Contextual)
     */
    default void destroy() {
        // No-op
    }

    /**
     *
     * @return the injectable bean for a CDI contextual instance or {@code null}
     */
    default InjectableBean<T> getBean() {
        return null;
    }

    /**
     * Delegates to {@link #destroy()} if the handle does not represent a CDI contextual instance or if it represents a
     * {@link Dependent} CDI contextual instance.
     */
    @Override
    default void close() {
        InjectableBean<T> bean = getBean();
        if (bean == null || Dependent.class.equals(bean.getScope())) {
            destroy();
        }
    }

}
