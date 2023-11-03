package io.quarkus.arc;

/**
 * An instance handle that can be used to store contextual instances in an {@link InjectableContext}.
 *
 * @param <T>
 */
public interface ContextInstanceHandle<T> extends InstanceHandle<T> {

    /**
     * Destroy the instance as defined by
     * {@link jakarta.enterprise.context.spi.Contextual#destroy(Object, jakarta.enterprise.context.spi.CreationalContext)}.
     */
    void destroy();

}
