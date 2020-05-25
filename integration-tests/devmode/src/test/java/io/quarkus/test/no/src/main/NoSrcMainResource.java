package io.quarkus.test.no.src.main;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
