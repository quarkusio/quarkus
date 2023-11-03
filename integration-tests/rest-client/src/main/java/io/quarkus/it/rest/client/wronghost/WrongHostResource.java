package io.quarkus.it.rest.client.wronghost;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/wrong-host")
public class WrongHostResource {

    @Inject
    @RestClient
    WrongHostClient client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String perform() throws IOException {
        return String.valueOf(client.invoke().getStatus());
    }
}
