package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class RootBResource {

    @GET
    @Path("b")
    public String b() {
        return "b";
    }

}
