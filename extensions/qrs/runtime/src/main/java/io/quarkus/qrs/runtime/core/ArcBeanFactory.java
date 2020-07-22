package io.quarkus.qrs.runtime.core;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ArcBeanFactory<T> implements BeanFactory<T> {

    private final BeanContainer.Factory<T> factory;

    public ArcBeanFactory(Class<T> target, BeanContainer beanContainer) {
        factory = beanContainer.instanceFactory(target);
    }

    @Override
    public BeanInstance<T> createInstance() {
        BeanContainer.Instance<T> instance = factory.create();
        return new BeanInstance<T>() {
            @Override
            public T getInstance() {
                return instance.get();
            }

            @Override
            public void close() {
                instance.close();
            }
        };
    }

    @Override
    public BeanInstance<T> createInstance(RequestContext requestContext) {
        return createInstance();
    }

    public static class Supplier {

    }
}
