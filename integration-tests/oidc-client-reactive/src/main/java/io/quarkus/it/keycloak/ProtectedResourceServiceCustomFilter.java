package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient
@RegisterProvider(OidcClientRequestCustomFilter.class)
@Path("/")
public interface ProtectedResourceServiceCustomFilter {

    @GET
    @Produces("text/plain")
    @Path("userName")
    Uni<String> getUserName();
}
