package io.quarkus.rest.client.reactive.lock.prevention;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/echo")
@Produces("text/plain")
@Consumes("text/plain")
public class TestResource {

    @GET
    public String gimmeSomething() {
        return "something";
    }
}
