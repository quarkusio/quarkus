package io.quarkus.oidc.client.filter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@OidcClientFilter
@RegisterRestClient
@Path("/")
public interface ProtectedResourceServiceConfigPropertyOidcClient {

    @GET
    String getUserName();
}
