package io.quarkus.arc.impl;

import static io.quarkus.arc.impl.CreationalContextImpl.unwrap;

import io.quarkus.arc.InjectableReferenceProvider;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;

/**
 * {@link Intercepted} {@link Bean} metadata provider.
 */
public class InterceptedBeanMetadataProvider implements InjectableReferenceProvider<Contextual<?>> {

    @Override
    public Contextual<?> get(CreationalContext<Contextual<?>> creationalContext) {
        // First attempt to obtain the creational context of the interceptor bean and then the creational context of the intercepted bean
        CreationalContextImpl<?> parent = unwrap(creationalContext).getParent();
        if (parent != null) {
            parent = parent.getParent();
            return parent != null ? parent.getContextual() : null;
        }
        return null;
    }

}
