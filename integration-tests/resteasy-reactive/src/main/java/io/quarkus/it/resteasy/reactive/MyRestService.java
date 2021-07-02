package io.quarkus.it.resteasy.reactive;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "my-service")
@Path("/reactive")
public interface MyRestService {

    @GET
    @Path("/hello")
    @Consumes(MediaType.TEXT_PLAIN)
    Uni<String> hello();

    @GET
    @Path("/pet")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Pet> pet();

}
