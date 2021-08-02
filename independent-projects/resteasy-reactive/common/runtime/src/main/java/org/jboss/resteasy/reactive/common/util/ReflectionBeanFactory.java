package org.jboss.resteasy.reactive.common.util;

import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ReflectionBeanFactory<T> implements BeanFactory<T> {
    private final String className;

    public ReflectionBeanFactory(String className) {
        this.className = className;
    }

    @Override
    public BeanInstance<T> createInstance() {
        try {
            T instance = (T) Class.forName(className, false, Thread.currentThread().getContextClassLoader())
                    .getDeclaredConstructor()
                    .newInstance();
            return new BeanInstance<T>() {
                @Override
                public T getInstance() {
                    return instance;
                }

                @Override
                public void close() {

                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
