package io.quarkus.it.resteasy.mutiny;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "my-service")
@Path("/mutiny")
public interface MyRestService {

    @GET
    @Path("/hello")
    Uni<String> hello();

    @GET
    @Path("/pet")
    Uni<Pet> pet();

}
