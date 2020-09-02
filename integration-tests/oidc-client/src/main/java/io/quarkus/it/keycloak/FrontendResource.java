package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceServiceOidcClient protectedResourceServiceOidcClient;

    @Inject
    @RestClient
    ProtectedResourceServiceOidcClient protectedResourceServiceRegisterProvider;

    @Inject
    @RestClient
    ProtectedResourceServiceNoOidcClient protectedResourceServiceNoOidcClient;

    @GET
    @Path("userOidcClient")
    public String userNameOidcClient() {
        return protectedResourceServiceOidcClient.getUserName();
    }

    @GET
    @Path("userRegisterProvider")
    public String userNameRegisterProvider() {
        return protectedResourceServiceRegisterProvider.getUserName();
    }

    @GET
    @Path("userNoOidcClient")
    public String userNameNoOidcClient() {
        return protectedResourceServiceNoOidcClient.getUserName();
    }
}
