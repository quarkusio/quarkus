package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient
@Path("/")
public interface ServiceWithoutToken {

    @GET
    @Produces("text/plain")
    Uni<String> getUserName();
}
