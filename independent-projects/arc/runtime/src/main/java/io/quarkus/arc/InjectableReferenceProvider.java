package io.quarkus.arc;

import jakarta.enterprise.context.spi.CreationalContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InjectableReferenceProvider<T> {

    /**
     *
     * @param creationalContext
     * @return a contextual reference
     */
    T get(CreationalContext<T> creationalContext);

}
