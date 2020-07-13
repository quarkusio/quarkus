package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.spi.EndpointFactory;

public class InstanceHandler implements RestHandler {

    private final EndpointFactory factory;

    public InstanceHandler(EndpointFactory factory) {
        this.factory = factory;
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        EndpointFactory.EndpointInstance instance = factory.createInstance(requestContext);
        requestContext.setEndpointInstance(instance);
    }
}
