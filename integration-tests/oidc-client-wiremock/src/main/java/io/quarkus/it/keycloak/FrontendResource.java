package io.quarkus.it.keycloak;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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

    @GET
    @Path("password-grant-public-client")
    @Produces("text/plain")
    public Uni<String> passwordGrantPublicClient() {
        return clients.getClient("password-grant-public-client").getTokens().onItem().transform(t -> t.getAccessToken());
    }

    @GET
    @Path("ciba-grant")
    @Produces("text/plain")
    public Uni<Response> cibaGrant(@QueryParam("authReqId") String authReqId) {
        return clients.getClient("ciba-grant").getTokens(Map.of("auth_req_id", authReqId))
                .onItem().transform(t -> Response.ok(t.getAccessToken()).build())
                .onFailure(OidcClientException.class).recoverWithItem(t -> Response.status(400).entity(t.getMessage()).build());
    }
}
