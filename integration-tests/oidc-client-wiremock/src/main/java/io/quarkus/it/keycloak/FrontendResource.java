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
    @RestClient
    ProtectedResourceServiceCrashTestClient protectedResourceServiceCrashTestClient;

    @Inject
    @RestClient
    JwtBearerAuthenticationOidcClient jwtBearerAuthenticationOidcClient;

    @Inject
    @RestClient
    JwtBearerFileAuthenticationOidcClient jwtBearerFileAuthenticationOidcClient;

    @Inject
    @NamedOidcClient("non-standard-response")
    Tokens tokens;

    @Inject
    @NamedOidcClient("configured-expires-in")
    Tokens tokensConfiguredExpiresIn;

    @Inject
    @NamedOidcClient("non-standard-response-without-header")
    OidcClient tokensWithoutHeader;

    @Inject
    @NamedOidcClient("jwtbearer-grant")
    OidcClient jwtBearerGrantClient;

    @Inject
    @RestClient
    ProtectedResourceServiceRefreshIntervalTestClient tokenRefreshIntervalTestClient;

    @Inject
    OidcClients clients;

    @GET
    @Path("echoToken")
    public String echoToken() {
        return protectedResourceServiceOidcClient.echoToken();
    }

    @GET
    @Path("crashTest")
    public String crashTest() {
        return protectedResourceServiceCrashTestClient.echoToken();
    }

    @GET
    @Path("tokenRefreshInterval")
    public String tokenRefreshInterval() {
        return tokenRefreshIntervalTestClient.echoToken();
    }

    @GET
    @Path("echoTokenJwtBearerGrant")
    public String echoTokenJwtBearerGrant() {
        return jwtBearerGrantClient.getTokens().await().indefinitely().getAccessToken();
    }

    @GET
    @Path("echoTokenJwtBearerAuthentication")
    public String echoTokenJwtBearerAuthentication() {
        return jwtBearerAuthenticationOidcClient.echoToken();
    }

    @GET
    @Path("echoTokenJwtBearerAuthenticationFromFile")
    public String echoTokenJwtBearerAuthenticationFromFile() {
        return jwtBearerFileAuthenticationOidcClient.echoToken();
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
    @Path("echoTokenConfiguredExpiresIn")
    public String echoTokenConfiguredExpiresIn() {
        try {
            return tokensConfiguredExpiresIn.getAccessToken() + " " + tokensConfiguredExpiresIn.getAccessTokenExpiresAt();
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
        return clients.getClient("refresh").refreshTokens(refreshToken, Map.of("extra_param", "extra_param_value"))
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

    @GET
    @Path("device-code-grant")
    @Produces("text/plain")
    public Uni<Response> deviceCodeGrant(@QueryParam("deviceCode") String deviceCode) {
        return clients.getClient("device-code-grant").getTokens(Map.of("device_code", deviceCode))
                .onItem().transform(t -> Response.ok(t.getAccessToken()).build())
                .onFailure(OidcClientException.class).recoverWithItem(t -> Response.status(401).build());
    }
}
