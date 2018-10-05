package org.jboss.protean.arc;

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
     * Destroy/release the instance. If this is a CDI contextual instance it's also removed from the underlying context.
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
     * Delegates to {@link #destroy()}.
     */
    default void close() {
        destroy();
    }

}
