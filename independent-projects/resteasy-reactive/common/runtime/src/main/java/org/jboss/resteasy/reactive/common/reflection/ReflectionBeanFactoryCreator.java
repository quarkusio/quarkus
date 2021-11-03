package org.jboss.resteasy.reactive.common.reflection;

import java.util.function.Function;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ReflectionBeanFactoryCreator<T> implements Function<String, BeanFactory<T>> {
    @Override
    public BeanFactory<T> apply(String className) {
        return new ReflectionBeanFactory<T>(className);
    }

}
