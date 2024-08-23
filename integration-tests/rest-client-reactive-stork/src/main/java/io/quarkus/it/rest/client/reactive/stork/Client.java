package io.quarkus.it.rest.client.reactive.stork;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "hello")
public interface Client {
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    String echo(String name);

    @GET
    @Path("/v2/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    String invoke(@PathParam("name") String name);
}
