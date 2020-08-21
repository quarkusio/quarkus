package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.spi.BeanFactory;

public class InstanceHandler implements RestHandler {

    /**
     * CDI Manages the lifecycle. If this is a per request resource then this will be a client proxy
     *
     */
    private final Object instance;

    public InstanceHandler(BeanFactory<Object> factory) {
        this.instance = factory.createInstance().getInstance();
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        requestContext.setEndpointInstance(instance);
    }
}
