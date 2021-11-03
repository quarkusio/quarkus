package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientException;
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
    @NamedOidcClient("non-standard-response-without-header")
    OidcClient tokensWithoutHeader;

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
        try {
            return tokens.getAccessToken() + " " + tokens.getRefreshToken();
        } catch (OidcClientException ex) {
            throw new WebApplicationException(401);
        }
    }

    @GET
    @Path("echoTokenNonStandardResponseWithoutHeader")
    public Uni<Tokens> echoTokenNonStandardResponseWithoutHeader() {
        return tokensWithoutHeader.getTokens().onFailure(OidcClientException.class)
                .transform(t -> new WebApplicationException(401));
    }

    @GET
    @Path("echoRefreshTokenOnly")
    @Produces("text/plain")
    public Uni<String> echoRefreshTokenOnly(@QueryParam("refreshToken") String refreshToken) {
        return clients.getClient("refresh").refreshTokens(refreshToken)
                .onItem().transform(t -> t.getAccessToken());
    }
}
