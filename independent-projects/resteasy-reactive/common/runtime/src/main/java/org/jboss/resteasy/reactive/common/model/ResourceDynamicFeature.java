package org.jboss.resteasy.reactive.common.model;

import jakarta.ws.rs.container.DynamicFeature;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceDynamicFeature {

    private String className;
    private BeanFactory<DynamicFeature> factory;

    public String getClassName() {
        return className;
    }

    public ResourceDynamicFeature setClassName(String className) {
        this.className = className;
        return this;
    }

    public BeanFactory<DynamicFeature> getFactory() {
        return factory;
    }

    public void setFactory(BeanFactory<DynamicFeature> factory) {
        this.factory = factory;
    }
}
