package io.quarkus.rest.server.runtime.handlers;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.spi.BeanFactory;

public class InstanceHandler implements ServerRestHandler {

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
