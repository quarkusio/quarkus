package org.jboss.resteasy.reactive.common.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class SingletonBeanFactory<T> implements BeanFactory<T> {

    static final Map<String, Object> INSTANCES = new ConcurrentHashMap<>();

    private String className;

    public SingletonBeanFactory(String className) {
        this.className = className;
    }

    public SingletonBeanFactory() {
    }

    @Override
    public String toString() {
        return "SingletonBeanFactory[" + className + "]";
    }

    @Override
    public BeanInstance<T> createInstance() {
        Object instance = INSTANCES.get(className);
        if (instance == null) {
            throw new RuntimeException("Singleton for " + className + " not found.");
        }
        return new BeanInstance<T>() {
            @Override
            public T getInstance() {
                return (T) instance;
            }

            @Override
            public void close() {

            }
        };
    }

    public String getClassName() {
        return className;
    }

    public SingletonBeanFactory<T> setClassName(String className) {
        this.className = className;
        return this;
    }

    public static void setInstance(String className, Object instance) {
        INSTANCES.put(className, instance);
    }
}
