package io.quarkus.it.resteasy.reactive;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
