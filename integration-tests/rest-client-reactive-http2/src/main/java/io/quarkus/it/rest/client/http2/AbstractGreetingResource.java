package io.quarkus.it.rest.client.http2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greet")
public abstract class AbstractGreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public abstract String hello();
}
