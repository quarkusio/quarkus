package io.quarkus.arc.impl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;
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
