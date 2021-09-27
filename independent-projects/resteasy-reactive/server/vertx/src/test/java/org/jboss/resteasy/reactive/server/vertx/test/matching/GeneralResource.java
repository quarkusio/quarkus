package org.jboss.resteasy.reactive.server.vertx.test.matching;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class GeneralResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{id}")
    public String hello(@PathParam("id") String id) {
        return "general:" + id;
    }
}