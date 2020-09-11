package io.quarkus.rest.runtime.model;

import javax.ws.rs.container.DynamicFeature;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceDynamicFeature {

    private BeanFactory<DynamicFeature> factory;

    public BeanFactory<DynamicFeature> getFactory() {
        return factory;
    }

    public void setFactory(BeanFactory<DynamicFeature> factory) {
        this.factory = factory;
    }
}
