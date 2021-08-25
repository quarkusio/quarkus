package io.quarkus.arc.impl;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

public class InjectionTargetFactoryImpl<T> implements InjectionTargetFactory<T> {
    public <T> InjectionTargetFactoryImpl(AnnotatedType<T> annotatedType) {

    }

    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean) {
        return null;
    }

    @Override
    public AnnotatedTypeConfigurator<T> configure() {
        return InjectionTargetFactory.super.configure();
    }
}
