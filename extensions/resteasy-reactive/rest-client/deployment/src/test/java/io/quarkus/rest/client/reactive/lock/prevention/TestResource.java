package io.quarkus.rest.client.reactive.lock.prevention;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/echo")
@Produces("text/plain")
@Consumes("text/plain")
public class TestResource {

    @GET
    public String gimmeSomething() {
        return "something";
    }
}
