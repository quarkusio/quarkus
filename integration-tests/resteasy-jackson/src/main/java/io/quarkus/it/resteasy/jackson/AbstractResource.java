package io.quarkus.it.resteasy.jackson;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/abstract")
public abstract class AbstractResource {

    @GET
    @Path("inherited")
    @Produces("text/plain")
    public String inherited() {
        return message();
    }

    public abstract String message();
}
