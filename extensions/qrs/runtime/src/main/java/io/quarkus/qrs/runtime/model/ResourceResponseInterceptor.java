package io.quarkus.qrs.runtime.model;

import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.qrs.runtime.spi.BeanFactory;

public class ResourceResponseInterceptor {

    private BeanFactory<ContainerResponseFilter> factory;
    private boolean writerSafe;

    public void setFactory(BeanFactory<ContainerResponseFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerResponseFilter> getFactory() {
        return factory;
    }

    public boolean isWriterSafe() {
        return writerSafe;
    }

    public void setWriterSafe(boolean writerSafe) {
        this.writerSafe = writerSafe;
    }
}
