package io.quarkus.resteasy.reactive.server.test.duplicate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("")
public class GreetingResource2 {

    @Path("hello-resteasy")
    @GET
    public String helloGet() {
        return "Hello duplicate RESTEasy";
    }

}
