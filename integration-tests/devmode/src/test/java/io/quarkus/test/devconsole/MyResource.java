package io.quarkus.test.devconsole;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/me")
public class MyResource {

    @Path("message")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        return "hello";
    }
}
