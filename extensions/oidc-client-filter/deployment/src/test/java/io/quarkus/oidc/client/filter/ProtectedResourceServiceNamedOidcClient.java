package io.quarkus.oidc.client.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@OidcClientFilter("named")
@RegisterRestClient
@Path("/")
public interface ProtectedResourceServiceNamedOidcClient {

    @GET
    String getUserName();
}
