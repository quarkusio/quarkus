package org.jboss.resteasy.reactive.server.vertx.test.matching;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello/{id}/bar")
public class SpecificResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("id") String id) {
        return "specific:" + id;
    }
}
