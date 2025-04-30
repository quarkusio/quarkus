package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("iface")
public interface InterfaceResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
