package org.jboss.resteasy.reactive.server.jaxrs;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ResourceContext;

public class QuarkusRestResourceContext implements ResourceContext {
    public static QuarkusRestResourceContext INSTANCE = new QuarkusRestResourceContext();

    @Override
    public <T> T getResource(Class<T> resourceClass) {
        return CDI.current().select(resourceClass).get();
    }

    @Override
    public <T> T initResource(T resource) {
        return resource;
    }
}
