package io.quarkus.rest.runtime.model;

import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceResponseInterceptor {

    private BeanFactory<ContainerResponseFilter> factory;

    public void setFactory(BeanFactory<ContainerResponseFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerResponseFilter> getFactory() {
        return factory;
    }
}
