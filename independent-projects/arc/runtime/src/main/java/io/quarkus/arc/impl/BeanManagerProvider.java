package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableReferenceProvider;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;

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
