package io.quarkus.resteasy.jackson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/patch")
public class PatchResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PatchMessage get() {
        return new PatchMessage().setMessage("Hello").setAuthor("Alice");
    }

    @PATCH
    @Consumes("application/merge-patch+json")
    @Produces(MediaType.APPLICATION_JSON)
    public PatchMessage patch(PatchMessage message) {
        return message;
    }
}
