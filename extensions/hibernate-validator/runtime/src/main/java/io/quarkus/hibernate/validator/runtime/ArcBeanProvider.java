package io.quarkus.hibernate.validator.runtime;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.literal.NamedLiteral;

import org.hibernate.validator.bean.BeanHolder;
import org.hibernate.validator.spi.bean.BeanNotFoundException;
import org.hibernate.validator.spi.bean.BeanProvider;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

final class ArcBeanProvider implements BeanProvider {

    private final ArcContainer container;

    ArcBeanProvider(ArcContainer container) {
        this.container = container;
    }

    @Override
    public <T> BeanHolder<T> forType(Class<T> typeReference) {
        return resolve(typeReference);
    }

    @Override
    public <T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String nameReference) {
        return resolve(typeReference, NamedLiteral.of(nameReference));
    }

    @Override
    public void close() {
        // The CDI container manages its own lifecycle.
    }

    private <T> BeanHolder<T> resolve(Class<T> typeReference, Annotation... qualifiers) {
        var instance = container.select(typeReference, qualifiers);
        if (instance.isUnsatisfied()) {
            throw new BeanNotFoundException("No matching CDI bean for type " + typeReference.getName());
        }
        // Obtain a lazy handle: validators are initialized at static init, but their dependencies may require runtime init.
        // getHandle() also reports ambiguous resolution instead of letting Hibernate Validator fall back to reflection.
        return new ArcBeanHolder<>(instance.getHandle());
    }

    private static final class ArcBeanHolder<T> implements BeanHolder<T> {

        private final InstanceHandle<T> handle;

        private ArcBeanHolder(InstanceHandle<T> handle) {
            this.handle = handle;
        }

        @Override
        public T get() {
            return handle.get();
        }

        @Override
        public void close() {
            if (Dependent.class.equals(handle.getBean().getScope())) {
                handle.destroy();
            }
        }
    }
}
