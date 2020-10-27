package io.quarkus.it.resteasy.jackson;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
