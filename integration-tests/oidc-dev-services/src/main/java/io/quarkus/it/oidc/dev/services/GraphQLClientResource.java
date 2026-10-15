package io.quarkus.it.oidc.dev.services;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/oidc-graphql-test")
public class GraphQLClientResource {

    @Inject
    TypesafeGraphQLClient defaultOidcClient;

    @Inject
    TypesafeGraphQLClientWithNamedOidcClient namedOidcClient;

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
}
