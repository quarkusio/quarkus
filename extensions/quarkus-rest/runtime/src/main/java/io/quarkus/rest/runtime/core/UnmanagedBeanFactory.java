package io.quarkus.rest.runtime.core;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class UnmanagedBeanFactory<T> implements BeanFactory<T> {

    private final T instance;

    public UnmanagedBeanFactory(T instance) {
        this.instance = instance;
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
