package org.jboss.resteasy.reactive.common.model;

import jakarta.ws.rs.core.Feature;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceFeature {

    private String className;

    public ResourceFeature setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getClassName() {
        return className;
    }

    private BeanFactory<Feature> factory;

    public BeanFactory<Feature> getFactory() {
        return factory;
    }

    public void setFactory(BeanFactory<Feature> factory) {
        this.factory = factory;
    }
}
