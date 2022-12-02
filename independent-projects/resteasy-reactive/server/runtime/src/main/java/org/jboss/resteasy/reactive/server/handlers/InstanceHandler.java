package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;

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
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    requestContext.requireCDIRequestScope();
                    instance = factory.createInstance().getInstance();
                }
            }
        }
        requestContext.setEndpointInstance(instance);
    }
}
