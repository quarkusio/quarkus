package io.quarkus.resteasy.reactive.server.test.duplicate;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("")
public class GreetingResource2 {

    @Path("hello-resteasy")
    @GET
    public String helloGet() {
        return "Hello duplicate RESTEasy";
    }

}
