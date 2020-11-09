package org.jboss.resteasy.reactive.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreMatchInterceptorContainer<T> extends InterceptorContainer<T> {
    private final List<ResourceInterceptor<T>> preMatchInterceptors = new ArrayList<>();

    public void addPreMatchInterceptor(ResourceInterceptor<T> interceptor) {
        preMatchInterceptors.add(interceptor);
    }

    public List<ResourceInterceptor<T>> getPreMatchInterceptors() {
        return preMatchInterceptors;
    }

    @Override
    public void sort() {
        super.sort();
        Collections.sort(preMatchInterceptors);
    }
}
