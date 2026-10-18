package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

public interface MultiInterfaceResLocatorIntf2 {
    @GET
    @Produces("text/plain")
    @Path("hello2")
    String resourceMethod2();
}
