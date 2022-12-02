package org.jboss.resteasy.reactive.server.vertx.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/hello")
public class HelloResource {

    @GET
    public String hello(@QueryParam("name") String name) {
        return "hello " + name;
    }

}
