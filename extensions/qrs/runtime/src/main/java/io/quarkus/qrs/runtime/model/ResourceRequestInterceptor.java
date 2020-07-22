package io.quarkus.qrs.runtime.model;

import io.quarkus.qrs.runtime.spi.EndpointFactory;

public class ResourceRequestInterceptor {

    private EndpointFactory factory;

    public void setFactory(EndpointFactory factory) {
        this.factory = factory;
    }

    public EndpointFactory getFactory() {
        return factory;
    }

}
