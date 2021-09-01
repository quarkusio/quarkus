package io.quarkus.resteasy.reactive.common.runtime;

import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.runtime.BeanContainer;

public class ArcBeanFactory<T> implements BeanFactory<T> {

    private final BeanContainer.Factory<T> factory;
    // for toString
    private final String targetClassName;

    public ArcBeanFactory(Class<T> target, BeanContainer beanContainer) {
        targetClassName = target.getName();
        factory = beanContainer.instanceFactory(target);
    }

    @Override
    public String toString() {
        return "ArcBeanFactory[" + targetClassName + "]";
    }

    @Override
    public BeanInstance<T> createInstance() {
        BeanContainer.Instance<T> instance;
        try {
            instance = factory.create();
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
        } catch (Exception e) {
            if (factory.getClass().getName().contains("DefaultInstanceFactory")) {
                throw new IllegalArgumentException(
                        "Unable to create class '" + targetClassName
                                + "'. To fix the problem, make sure this class is a CDI bean.",
                        e);
            }
            throw e;
        }
    }
}
