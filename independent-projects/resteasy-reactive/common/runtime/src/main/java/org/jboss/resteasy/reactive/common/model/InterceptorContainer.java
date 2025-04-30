package org.jboss.resteasy.reactive.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.jboss.resteasy.reactive.spi.BeanFactory;

public class InterceptorContainer<T> {

    private final List<ResourceInterceptor<T>> globalResourceInterceptors = new ArrayList<>();
    private final List<ResourceInterceptor<T>> nameResourceInterceptors = new ArrayList<>();

    public void addGlobalRequestInterceptor(ResourceInterceptor<T> interceptor) {
        this.globalResourceInterceptors.add(interceptor);
    }

    public void addNameRequestInterceptor(ResourceInterceptor<T> interceptor) {
        this.nameResourceInterceptors.add(interceptor);
    }

    public List<ResourceInterceptor<T>> getGlobalResourceInterceptors() {
        return globalResourceInterceptors;
    }

    public List<ResourceInterceptor<T>> getNameResourceInterceptors() {
        return nameResourceInterceptors;
    }

    public void sort() {
        Collections.sort(globalResourceInterceptors);
        Collections.sort(nameResourceInterceptors);
    }

    public boolean isEmpty() {
        return globalResourceInterceptors.isEmpty() && nameResourceInterceptors.isEmpty();
    }

    public ResourceInterceptor<T> create() {
        return new ResourceInterceptor<>();
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (ResourceInterceptor<T> i : globalResourceInterceptors) {
            if (i.getFactory() == null) {
                i.setFactory((BeanFactory<T>) factoryCreator.apply(i.getClassName()));
            }
        }
        for (ResourceInterceptor<T> i : nameResourceInterceptors) {
            if (i.getFactory() == null) {
                i.setFactory((BeanFactory<T>) factoryCreator.apply(i.getClassName()));
            }
        }
    }

    public static class Reversed<T> extends InterceptorContainer<T> {

        public ResourceInterceptor<T> create() {
            return new ResourceInterceptor.Reversed();
        }
    }
}
