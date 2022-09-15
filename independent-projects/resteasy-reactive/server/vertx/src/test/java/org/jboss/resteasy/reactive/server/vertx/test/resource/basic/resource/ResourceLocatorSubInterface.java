package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
public interface ResourceLocatorSubInterface extends ResourceLocatorRootInterface {

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    String post(String s);
}
