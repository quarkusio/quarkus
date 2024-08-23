package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class RootAResource {

    @GET
    @Path("a")
    public String a() {
        return "a";
    }

}
