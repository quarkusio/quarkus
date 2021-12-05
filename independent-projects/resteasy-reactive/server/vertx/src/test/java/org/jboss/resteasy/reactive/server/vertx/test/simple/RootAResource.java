package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class RootAResource {

    @GET
    @Path("a")
    public String a() {
        return "a";
    }

}
