package io.quarkus.qrs.runtime.model;

import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ResourceReader<T> {

    private BeanFactory<MessageBodyReader<T>> factory;

    public void setFactory(BeanFactory<MessageBodyReader<T>> factory) {
        this.factory = factory;
    }

    public BeanFactory<MessageBodyReader<T>> getFactory() {
        return factory;
    }

}
