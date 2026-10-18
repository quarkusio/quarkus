package org.jboss.resteasy.reactive.server.vertx.test.matching;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GeneralResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{id}")
    public String hello(@PathParam("id") String id) {
        return "general:" + id;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("prefix-{id}")
    public String prefixedHello(@PathParam("id") String id) {
        return "prefix:" + id;
    }
}
