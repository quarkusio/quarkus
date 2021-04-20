package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
