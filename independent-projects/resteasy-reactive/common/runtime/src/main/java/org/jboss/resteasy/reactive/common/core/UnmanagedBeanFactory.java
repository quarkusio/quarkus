package org.jboss.resteasy.reactive.common.core;

import org.jboss.resteasy.reactive.spi.BeanFactory;

public class UnmanagedBeanFactory<T> implements BeanFactory<T> {

    private final T instance;

    public UnmanagedBeanFactory(T instance) {
        this.instance = instance;
    }

    @Override
    public String toString() {
        return "UnmanagedBeanFactory[" + instance + "]";
    }

    @Override
    public BeanInstance<T> createInstance() {
        return new BeanInstance<T>() {
            @Override
            public T getInstance() {
                return instance;
            }

            @Override
            public void close() {

            }
        };
    }
}
