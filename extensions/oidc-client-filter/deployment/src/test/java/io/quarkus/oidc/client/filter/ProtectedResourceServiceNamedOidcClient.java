package io.quarkus.oidc.client.filter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@OidcClientFilter("named")
@RegisterRestClient
@Path("/")
public interface ProtectedResourceServiceNamedOidcClient {

    @GET
    String getUserName();
}
