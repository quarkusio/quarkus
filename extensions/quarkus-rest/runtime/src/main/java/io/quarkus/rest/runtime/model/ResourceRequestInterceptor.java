package io.quarkus.rest.runtime.model;

import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceRequestInterceptor {

    private BeanFactory<ContainerRequestFilter> factory;
    private boolean preMatching;

    public void setFactory(BeanFactory<ContainerRequestFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerRequestFilter> getFactory() {
        return factory;
    }

    public void setPreMatching(boolean preMatching) {
        this.preMatching = preMatching;
    }

    public boolean isPreMatching() {
        return preMatching;
    }
}
