package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceServiceOidcClient protectedResourceServiceOidcClient;

    @Inject
    @NamedOidcClient("non-standard-response")
    Tokens tokens;

    @Inject
    OidcClients clients;

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

    @GET
    @Path("echoRefreshTokenOnly")
    @Produces("text/plain")
    public Uni<String> echoRefreshTokenOnly(@QueryParam("refreshToken") String refreshToken) {
        return clients.getClient("refresh").refreshTokens(refreshToken)
                .onItem().transform(t -> t.getAccessToken());
    }
}
