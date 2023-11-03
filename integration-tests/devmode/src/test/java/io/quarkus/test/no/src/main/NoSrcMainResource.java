package io.quarkus.test.no.src.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class NoSrcMainResource {

    @ConfigProperty(name = "test.message")
    String message;

    @Path("message")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        return message;
    }
}
