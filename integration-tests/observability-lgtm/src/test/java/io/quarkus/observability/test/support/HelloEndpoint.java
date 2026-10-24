package io.quarkus.observability.test.support;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/hello")
public class HelloEndpoint {
    @GET
    public String hello() {
        return "hello";
    }
}
