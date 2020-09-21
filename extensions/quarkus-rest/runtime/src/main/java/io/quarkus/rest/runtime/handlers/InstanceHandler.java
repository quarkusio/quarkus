package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.BeanFactory;

public class InstanceHandler implements RestHandler {

    /**
     * CDI Manages the lifecycle
     *
     */
    private final Object instance;

    public InstanceHandler(BeanFactory<Object> factory) {
        this.instance = factory.createInstance().getInstance();
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.setEndpointInstance(instance);
    }
}
