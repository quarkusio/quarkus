package io.quarkus.arc;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

/**
 * {@link Bean} metadata provider.
 *
 * @author Martin Kouba
 */
public class BeanMetadataProvider implements InjectableReferenceProvider<InjectableBean<?>> {

    private final String beanId;

    public BeanMetadataProvider(String beanId) {
        this.beanId = beanId;
    }

    @Override
    public InjectableBean<?> get(CreationalContext<InjectableBean<?>> creationalContext) {
        return Arc.container().bean(beanId);
    }

}
