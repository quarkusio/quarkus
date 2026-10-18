package io.quarkus.undertow.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;

public class QuarkusInstanceFactory<T> implements InstanceFactory<T> {

    private final BeanContainer.Factory<T> factory;

    public QuarkusInstanceFactory(BeanContainer.Factory<T> factory) {
        this.factory = factory;
    }

    @Override
    public InstanceHandle<T> createInstance() throws InstantiationException {
        BeanContainer.Instance<T> instance = factory.create();
        return new InstanceHandle<T>() {
            @Override
            public T getInstance() {
                return instance.get();
            }

            @Override
            public void release() {
                instance.close();
            }
        };
    }
}
