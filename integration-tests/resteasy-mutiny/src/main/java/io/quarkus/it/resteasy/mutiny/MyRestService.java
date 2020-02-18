package io.quarkus.it.resteasy.mutiny;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

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
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Pet> pet();

}
