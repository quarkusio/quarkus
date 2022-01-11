package org.jboss.resteasy.reactive.server.vertx.test.path;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path(value = "hello test")
public class HelloSpaceResource {

    @GET
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/nested test")
    public String nested() {
        return "world hello";
    }
}
