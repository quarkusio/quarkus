package io.quarkus.qrs.runtime.model;

import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ResourceWriter<T> {

    private BeanFactory<MessageBodyWriter<T>> factory;

    public void setFactory(BeanFactory<MessageBodyWriter<T>> factory) {
        this.factory = factory;
    }

    public BeanFactory<MessageBodyWriter<T>> getFactory() {
        return factory;
    }

}
