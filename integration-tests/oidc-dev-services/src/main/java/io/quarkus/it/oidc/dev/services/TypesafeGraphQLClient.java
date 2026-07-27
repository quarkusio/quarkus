package io.quarkus.it.oidc.dev.services;

import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "typesafe-oidc-client")
public interface TypesafeGraphQLClient {

    @Query
    String principalName();
}
