package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@RegisterProvider(OidcClientRequestCustomFilter.class)
@Path("/")
public interface ProtectedResourceServiceNamedOidcClient {

    @GET
    String getUserName();
}
