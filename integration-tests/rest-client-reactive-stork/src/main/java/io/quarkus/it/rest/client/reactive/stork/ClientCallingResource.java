package io.quarkus.it.rest.client.reactive.stork;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String invoke(@PathParam("name") String name) {
        return client.invoke(name + "/" + name);
    }
}
