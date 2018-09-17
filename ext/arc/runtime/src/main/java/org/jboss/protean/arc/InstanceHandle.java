package org.jboss.protean.arc;

/**
 * Represents a contextual instance handle.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InstanceHandle<T> extends AutoCloseable {

    /**
     *
     * @return {@code true} if there is exactly one bean that matches the required type and qualifiers, {@code false} otherwise
     */
    boolean isAvailable();

    /**
     *
     * @return an injected instance of {@code T}
     */
    T get();

    /**
     * Destroys the underlying injected instance.
     *
     * @see javax.enterprise.context.spi.Contextual#destroy(Object, javax.enterprise.context.spi.CreationalContext)
     */
    void release();

    default void close() {
        release();
    }

}
