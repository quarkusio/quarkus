package io.quarkus.rest.runtime.model;

import javax.ws.rs.core.Feature;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceFeature {

    private BeanFactory<Feature> factory;

    public BeanFactory<Feature> getFactory() {
        return factory;
    }

    public void setFactory(BeanFactory<Feature> factory) {
        this.factory = factory;
    }
}
