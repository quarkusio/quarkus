package io.quarkus.oidc.client.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/named-oidc-client")
public class NamedOidcClientResource {

    @Inject
    @RestClient
    ProtectedResourceServiceNamedOidcClient protectedResourceServiceNamedOidcClient;

    @GET
    @Path("user-name")
    public String userName() {
        return protectedResourceServiceNamedOidcClient.getUserName();
    }
}
