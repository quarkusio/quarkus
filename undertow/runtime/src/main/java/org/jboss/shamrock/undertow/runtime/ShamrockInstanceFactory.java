package org.jboss.shamrock.undertow.runtime;

import org.jboss.shamrock.injection.InjectionInstance;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;

public class ShamrockInstanceFactory<T> implements InstanceFactory<T> {

    private final InjectionInstance<T> injectionInstance;

    public ShamrockInstanceFactory(InjectionInstance<T> injectionInstance) {
        this.injectionInstance = injectionInstance;
    }


    @Override
    public InstanceHandle<T> createInstance() throws InstantiationException {
        return new InstanceHandle<T>() {
            @Override
            public T getInstance() {
                return injectionInstance.newInstance();
            }

            @Override
            public void release() {

            }
        };
    }
}
