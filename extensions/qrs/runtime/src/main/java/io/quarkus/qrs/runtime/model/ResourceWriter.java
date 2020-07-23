package io.quarkus.qrs.runtime.model;

import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ResourceWriter<T> {

    private BeanFactory<MessageBodyWriter<T>> factory;
    private boolean buildTimeSelectable;

    public void setFactory(BeanFactory<MessageBodyWriter<T>> factory) {
        this.factory = factory;
    }

    public BeanFactory<MessageBodyWriter<T>> getFactory() {
        return factory;
    }

    public boolean isBuildTimeSelectable() {
        return buildTimeSelectable;
    }

    public void setBuildTimeSelectable(boolean buildTimeSelectable) {
        this.buildTimeSelectable = buildTimeSelectable;
    }
}
