package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.filter.OidcClientRequestFilter;

@RegisterRestClient
@RegisterProvider(OidcClientRequestFilter.class)
@Path("/")
public interface ProtectedResourceServiceRegisterProvider {

    @GET
    String getUserName();
}
