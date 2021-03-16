package org.jboss.resteasy.reactive.common.core;

import java.util.function.Supplier;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class LazyUnmanagedBeanFactory<T> implements BeanFactory<T> {

    private final Supplier<T> instanceSupplier;
    private volatile T instance = null;

    public LazyUnmanagedBeanFactory(Supplier<T> instanceSupplier) {
        this.instanceSupplier = instanceSupplier;
    }

    @Override
    public String toString() {
        return "UnmanagedBeanFactory[" + instanceSupplier + "]";
    }

    @Override
    public BeanInstance<T> createInstance() {
        return new BeanInstance<T>() {
            @Override
            public T getInstance() {
                if (instance == null) {
                    instance = instanceSupplier.get();
                }
                return instance;
            }

            @Override
            public void close() {

            }
        };
    }
}
