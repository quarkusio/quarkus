package io.quarkus.arc.processor.bcextensions;

import java.util.List;

import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanBuilder;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserverBuilder;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Types;

class SyntheticComponentsImpl implements SyntheticComponents {
    final List<SyntheticBeanBuilderImpl<?>> syntheticBeans;
    final List<SyntheticObserverBuilderImpl<?>> syntheticObservers;
    final DotName extensionClass;

    SyntheticComponentsImpl(List<SyntheticBeanBuilderImpl<?>> syntheticBeans,
            List<SyntheticObserverBuilderImpl<?>> syntheticObservers, DotName extensionClass) {
        this.syntheticBeans = syntheticBeans;
        this.syntheticObservers = syntheticObservers;
        this.extensionClass = extensionClass;
    }

    @Override
    public <T> SyntheticBeanBuilder<T> addBean(Class<T> implementationClass) {
        SyntheticBeanBuilderImpl<T> builder = new SyntheticBeanBuilderImpl<>(implementationClass);
        syntheticBeans.add(builder);
        return builder;
    }

    @Override
    public <T> SyntheticObserverBuilder<T> addObserver(Class<T> eventType) {
        org.jboss.jandex.Type jandexType = Types.jandexType(eventType);
        SyntheticObserverBuilderImpl<T> builder = new SyntheticObserverBuilderImpl<>(extensionClass, jandexType);
        syntheticObservers.add(builder);
        return builder;
    }

    @Override
    public <T> SyntheticObserverBuilder<T> addObserver(Type eventType) {
        org.jboss.jandex.Type jandexType = ((TypeImpl<?>) eventType).jandexType;
        SyntheticObserverBuilderImpl<T> builder = new SyntheticObserverBuilderImpl<>(extensionClass, jandexType);
        syntheticObservers.add(builder);
        return builder;
    }
}
