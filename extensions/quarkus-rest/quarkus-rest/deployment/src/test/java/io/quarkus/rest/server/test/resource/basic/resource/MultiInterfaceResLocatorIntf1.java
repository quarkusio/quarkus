package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public interface MultiInterfaceResLocatorIntf1 {
    @GET
    @Produces("text/plain")
    @Path("hello1")
    String resourceMethod1();
}
