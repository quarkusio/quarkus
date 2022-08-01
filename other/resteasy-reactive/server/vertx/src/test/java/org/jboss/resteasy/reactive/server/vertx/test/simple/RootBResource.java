package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class RootBResource {

    @GET
    @Path("b")
    public String b() {
        return "b";
    }

}
