package io.quarkus.it.rest.client.reactive.stork;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
