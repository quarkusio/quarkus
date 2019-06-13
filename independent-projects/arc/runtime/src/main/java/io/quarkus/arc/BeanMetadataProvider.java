package io.quarkus.arc;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

/**
 * Dummy {@link Bean} provider.
 *
 * @author Martin Kouba
 */
public class BeanMetadataProvider<T> implements InjectableReferenceProvider<T> {

    @Override
    public T get(CreationalContext<T> creationalContext) {
        // TODO log a warning
        return null;
    }

}
