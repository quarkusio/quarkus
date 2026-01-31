package io.quarkus.oidc.client.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.resteasy.filter.OidcClientRequestFilter;

@RegisterProvider(OidcClientRequestFilter.class)
@RegisterRestClient
@Path("/")
public interface ProtectedResourceServiceCustomProviderConfigPropOidcClient {

    @GET
    String getUserName();
}
