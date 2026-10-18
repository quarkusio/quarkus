package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/")
public class MultiInterfaceResLocatorResource {
    @Produces("text/plain")
    @Path("test")
    public Object resourceLocator() {
        return new MultiInterfaceResLocatorSubresource();
    }
}
