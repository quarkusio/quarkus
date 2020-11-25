package org.jboss.resteasy.reactive.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public ResourceInterceptor<T> create() {
        return new ResourceInterceptor<>();
    }

    public static class Reversed<T> extends InterceptorContainer<T> {

        public ResourceInterceptor<T> create() {
            return new ResourceInterceptor.Reversed();
        }
    }
}
