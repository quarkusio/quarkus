package io.quarkus.oidc.client.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/config-property-oidc-client")
public class ExtendedOidcClientRequestFilterResource {

    @Inject
    @RestClient
    ProtectedResourceServiceConfigPropertyOidcClient protectedResourceServiceConfigPropertyOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceExtendedOidcClientRequestFilter protectedResourceServiceExtendedOidcClientRequestFilter;

    @GET
    @Path("/annotation/user-name")
    public String userName() {
        return protectedResourceServiceConfigPropertyOidcClient.getUserName();
    }

    @GET
    @Path("/extended-provider/user-name")
    public String extendedOidcClientRequestFilterUserName() {
        return protectedResourceServiceExtendedOidcClientRequestFilter.getUserName();
    }
}
