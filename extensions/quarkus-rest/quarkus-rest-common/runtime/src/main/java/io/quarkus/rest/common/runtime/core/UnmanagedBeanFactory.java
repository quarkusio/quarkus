package io.quarkus.rest.common.runtime.core;

import io.quarkus.rest.spi.BeanFactory;

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
