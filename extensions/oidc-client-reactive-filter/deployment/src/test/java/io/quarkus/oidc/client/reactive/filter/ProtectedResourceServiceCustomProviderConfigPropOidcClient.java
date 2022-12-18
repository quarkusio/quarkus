package io.quarkus.oidc.client.reactive.filter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterRestClient
@Path("/")
public interface ProtectedResourceServiceCustomProviderConfigPropOidcClient {

    @GET
    String getUserName();
}
