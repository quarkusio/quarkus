package org.jboss.resteasy.reactive.server.vertx.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/hello")
public class HelloResource {

    @GET
    public String hello(@QueryParam("name") String name) {
        return "hello " + name;
    }

}
