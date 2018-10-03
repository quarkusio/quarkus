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
     * @return an injected instance of {@code T} or {@code null}
     */
    T get();

    /**
     * Destroys the instance and removes the instance from the underlying context.
     *
     */
    void destroy();

    /**
     *
     * @return the injectable bean
     */
    InjectableBean<T> getBean();

    /**
     * Delegates to {@link #destroy()}.
     */
    default void close() {
        destroy();
    }

}
