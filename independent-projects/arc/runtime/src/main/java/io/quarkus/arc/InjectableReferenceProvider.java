package io.quarkus.arc;

import javax.enterprise.context.spi.CreationalContext;

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
