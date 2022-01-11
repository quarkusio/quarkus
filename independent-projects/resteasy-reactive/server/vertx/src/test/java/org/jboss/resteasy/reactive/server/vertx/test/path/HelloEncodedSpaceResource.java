package org.jboss.resteasy.reactive.server.vertx.test.path;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path(value = "hello%20test")
public class HelloEncodedSpaceResource {

    @GET
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/nested%20test")
    public String nested() {
        return "world hello";
    }
}
