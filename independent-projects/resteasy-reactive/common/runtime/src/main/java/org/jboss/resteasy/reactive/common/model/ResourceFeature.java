package org.jboss.resteasy.reactive.common.model;

import javax.ws.rs.core.Feature;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceFeature {

    private BeanFactory<Feature> factory;

    public BeanFactory<Feature> getFactory() {
        return factory;
    }

    public void setFactory(BeanFactory<Feature> factory) {
        this.factory = factory;
    }
}
