package io.quarkus.it.oidc.dev.services;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Path("/oidc-graphql-test")
public class GraphQLClientResource {

    private final AtomicInteger tokenExchanges = new AtomicInteger();

    @Inject
    TypesafeGraphQLClient defaultOidcClient;

    @Inject
    TypesafeGraphQLClientWithNamedOidcClient namedOidcClient;

    @Inject
    TypesafeGraphQLClientWithTokenPropagation tokenPropagationClient;

    @Inject
    TypesafeGraphQLClientWithTokenExchange tokenExchangeClient;

    @GET
    @Path("/default")
    public String callWithDefaultOidcClient() {
        return defaultOidcClient.principalName();
    }

    @GET
    @Path("/named")
    public String callWithNamedOidcClient() {
        return namedOidcClient.principalName();
    }

    @GET
    @Path("/token-propagation")
    public String callWithPropagatedToken() {
        return tokenPropagationClient.principalName();
    }

    @GET
    @Path("/token-exchange")
    public String callWithExchangedToken() {
        return tokenExchangeClient.principalName();
    }

    /**
     * Mocks the token endpoint of the 'token-exchange' OIDC client, the exchanged token is the subject token.
     */
    @POST
    @Path("/token-endpoint")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String exchangeToken(@FormParam("subject_token") String subjectToken) {
        tokenExchanges.incrementAndGet();
        return new JsonObject().put("access_token", subjectToken).encode();
    }

    @GET
    @Path("/token-exchanges")
    public int getTokenExchanges() {
        return tokenExchanges.get();
    }
}
