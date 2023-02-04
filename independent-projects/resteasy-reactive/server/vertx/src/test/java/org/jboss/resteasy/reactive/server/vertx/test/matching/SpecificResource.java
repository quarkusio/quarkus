package org.jboss.resteasy.reactive.server.vertx.test.matching;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello/{id}/bar")
public class SpecificResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("id") String id) {
        return "specific:" + id;
    }
}
