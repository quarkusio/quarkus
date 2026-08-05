package io.quarkus.it.oidc.dev.services;

import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "typesafe-named-oidc-client")
@OidcClientFilter("graphql-oidc-client")
public interface TypesafeGraphQLClientWithNamedOidcClient {

    @Query
    String principalName();
}
