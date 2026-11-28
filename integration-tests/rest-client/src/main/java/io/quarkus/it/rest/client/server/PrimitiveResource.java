package io.quarkus.it.rest.client.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/primitive")
public class PrimitiveResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String value(@QueryParam("value") String value) {
        return value;
    }
}
