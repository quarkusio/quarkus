package io.quarkus.it.resteasy.jackson;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/message")
public class MessageResource {

    @ConfigProperty(name = "message")
    String message;

    @GET
    public String message() {
        return message;
    }

}
