package io.quarkus.qrs.runtime.model;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ResourceReader<T> {

    private BeanFactory<MessageBodyReader<T>> factory;
    private List<String> mediaTypes = new ArrayList<>();

    public void setFactory(BeanFactory<MessageBodyReader<T>> factory) {
        this.factory = factory;
    }

    public BeanFactory<MessageBodyReader<T>> getFactory() {
        return factory;
    }

    public List<String> getMediaTypes() {
        return mediaTypes;
    }

    public ResourceReader<T> setMediaTypes(List<String> mediaTypes) {
        this.mediaTypes = mediaTypes;
        return this;
    }
}
