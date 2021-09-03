package io.quarkus.arc.impl;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

public class InjectionTargetFactoryImpl<T> implements InjectionTargetFactory<T> {
    private AnnotatedType<T> annotatedType;
    private BeanManager beanManager;

    public InjectionTargetFactoryImpl(AnnotatedType<T> annotatedType, BeanManager beanManager) {
        this.annotatedType = annotatedType;
        this.beanManager = beanManager;
    }

    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean) {
        return new InjectionTargetImpl(bean, beanManager, annotatedType, this);
    }

    @Override
    public AnnotatedTypeConfigurator<T> configure() {
        throw new UnsupportedOperationException();
    }
}
