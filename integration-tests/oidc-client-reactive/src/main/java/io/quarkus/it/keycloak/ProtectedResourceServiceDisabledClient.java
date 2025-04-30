package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.mutiny.Uni;

@RegisterRestClient
@OidcClientFilter("disabled-client")
@Path("/")
public interface ProtectedResourceServiceDisabledClient {

    @GET
    @Produces("text/plain")
    @Path("userNameReactive")
    Uni<String> getUserName();
}
