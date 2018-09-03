package org.jboss.protean.arc;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InstanceHandle<T> extends AutoCloseable {

    boolean isAvailable();

    T get();

    void release();

    default void close() {
        release();
    }

}
