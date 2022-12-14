package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.mutiny.Uni;

@RegisterRestClient
@OidcClientFilter("named-client")
@Path("/")
public interface ProtectedResourceServiceNamedFilter {

    @GET
    @Produces("text/plain")
    @Path("userNameReactive")
    Uni<String> getUserName();
}
