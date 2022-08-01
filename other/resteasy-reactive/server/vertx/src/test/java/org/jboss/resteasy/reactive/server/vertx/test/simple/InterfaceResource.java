package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("iface")
public interface InterfaceResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
