package io.quarkus.it.rest.client.wronghost;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
