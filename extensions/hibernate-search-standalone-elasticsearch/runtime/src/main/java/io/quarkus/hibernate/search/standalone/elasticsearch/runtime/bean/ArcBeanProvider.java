package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean;

import jakarta.enterprise.inject.literal.NamedLiteral;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;

import io.quarkus.arc.ArcContainer;

public class ArcBeanProvider implements BeanProvider {
    private final ArcContainer delegate;

    public ArcBeanProvider(ArcContainer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public <T> BeanHolder<T> forType(Class<T> typeReference) {
        var instance = delegate.instance(typeReference);
        if (!instance.isAvailable()) {
            throw new BeanNotFoundException("No matching bean in CDI context for type " + instance);
        }
        return new ArcBeanHolder<>(instance);
    }

    @Override
    public <T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String nameReference) {
        var instance = delegate.instance(typeReference, NamedLiteral.of(nameReference));
        if (!instance.isAvailable()) {
            throw new BeanNotFoundException(
                    "No matching bean in CDI context for type " + typeReference + " and @Named(" + nameReference + ")");
        }
        return new ArcBeanHolder<>(instance);
    }
}
