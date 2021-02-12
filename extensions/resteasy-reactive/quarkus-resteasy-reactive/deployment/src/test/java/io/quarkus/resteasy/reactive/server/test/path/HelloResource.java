package io.quarkus.resteasy.reactive.server.test.path;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/hello")
public class HelloResource {

    @GET
    public String hello() {
        return "hello";
    }
}
