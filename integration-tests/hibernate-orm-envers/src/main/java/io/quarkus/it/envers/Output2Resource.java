package io.quarkus.it.envers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("output2")
public class Output2Resource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Message2 out() {
        return new Message2("test");
    }
}
