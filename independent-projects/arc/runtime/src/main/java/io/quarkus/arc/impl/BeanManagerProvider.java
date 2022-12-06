package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.InjectableReferenceProvider;

/**
 * {@link BeanManager} provider.
 *
 * @author Martin Kouba
 */
public class BeanManagerProvider<T> implements InjectableReferenceProvider<BeanManager> {

    @Override
    public BeanManager get(CreationalContext<BeanManager> creationalContext) {
        return BeanManagerImpl.INSTANCE.get();
    }

}
