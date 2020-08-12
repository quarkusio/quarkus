package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.spi.BeanFactory;

public class InstanceHandler implements RestHandler {

    private final BeanFactory<Object> factory;

    public InstanceHandler(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        BeanFactory.BeanInstance<Object> instance = factory.createInstance();
        requestContext.setEndpointInstance(instance);
    }
}
