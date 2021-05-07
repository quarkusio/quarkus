package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.Tokens;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceServiceOidcClient protectedResourceServiceOidcClient;

    @Inject
    @NamedOidcClient("non-standard-response")
    Tokens tokens;

    @GET
    @Path("echoToken")
    public String echoToken() {
        return protectedResourceServiceOidcClient.echoToken();
    }

    @GET
    @Path("echoTokenNonStandardResponse")
    public String echoTokenNonStandardResponse() {
        return tokens.getAccessToken() + " " + tokens.getRefreshToken();
    }
}
