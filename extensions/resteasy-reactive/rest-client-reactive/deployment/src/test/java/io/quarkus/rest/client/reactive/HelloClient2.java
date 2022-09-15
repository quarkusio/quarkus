package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "hello2")
public interface HelloClient2 {
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/")
    String echo(String name);

    @GET
    String bug18977();

    @GET
    @Path("delay")
    Uni<String> delay();
}
