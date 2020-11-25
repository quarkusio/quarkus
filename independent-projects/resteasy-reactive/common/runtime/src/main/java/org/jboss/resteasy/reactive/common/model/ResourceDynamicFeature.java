package org.jboss.resteasy.reactive.common.model;

import javax.ws.rs.container.DynamicFeature;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceDynamicFeature {

    private BeanFactory<DynamicFeature> factory;

    public BeanFactory<DynamicFeature> getFactory() {
        return factory;
    }

    public void setFactory(BeanFactory<DynamicFeature> factory) {
        this.factory = factory;
    }
}
