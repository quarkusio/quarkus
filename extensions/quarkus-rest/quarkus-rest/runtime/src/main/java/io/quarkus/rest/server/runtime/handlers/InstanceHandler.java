package io.quarkus.rest.server.runtime.handlers;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.spi.BeanFactory;

public class InstanceHandler implements ServerRestHandler {

    /**
     * CDI Manages the lifecycle
     *
     */
    private volatile Object instance;
    private final BeanFactory<Object> factory;

    public InstanceHandler(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = factory.createInstance().getInstance();
                }
            }
        }
        requestContext.setEndpointInstance(instance);
    }
}
