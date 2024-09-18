package io.quarkus.oidc.client.reactive.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterRestClient
@Path("/")
public interface ProtectedResourceServiceCustomProviderConfigPropOidcClient {

    @GET
    String getUserName();

    @GET
    @Path("/anonymous")
    String getAnonymousUserName();
}
