package org.jboss.resteasy.reactive.server.jaxrs;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ResourceContext;

public class ResourceContextImpl implements ResourceContext {
    public static ResourceContextImpl INSTANCE = new ResourceContextImpl();

    @Override
    public <T> T getResource(Class<T> resourceClass) {
        return CDI.current().select(resourceClass).get();
    }

    @Override
    public <T> T initResource(T resource) {
        return resource;
    }
}
