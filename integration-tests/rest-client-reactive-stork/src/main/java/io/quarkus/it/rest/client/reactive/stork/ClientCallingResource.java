package io.quarkus.it.rest.client.reactive.stork;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client")
@ApplicationScoped
public class ClientCallingResource {

    @RestClient
    Client client;

    @GET
    public String passThrough() {
        return client.echo("World!");
    }
}
