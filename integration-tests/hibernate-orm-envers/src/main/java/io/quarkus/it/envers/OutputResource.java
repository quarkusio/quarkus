package io.quarkus.it.envers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("output")
public class OutputResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Message out() {
        return new Message("test");
    }
}
