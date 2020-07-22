package io.quarkus.qrs.runtime.model;

import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ResourceRequestInterceptor {

    private BeanFactory<ContainerRequestFilter> factory;

    public void setFactory(BeanFactory<ContainerRequestFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerRequestFilter> getFactory() {
        return factory;
    }

}
