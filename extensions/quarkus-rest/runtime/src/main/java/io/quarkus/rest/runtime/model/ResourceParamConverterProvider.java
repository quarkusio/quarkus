package io.quarkus.rest.runtime.model;

import javax.ws.rs.ext.ParamConverterProvider;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceParamConverterProvider {

    private BeanFactory<ParamConverterProvider> factory;

    public void setFactory(BeanFactory<ParamConverterProvider> factory) {
        this.factory = factory;
    }

    public BeanFactory<ParamConverterProvider> getFactory() {
        return factory;
    }
}
