package io.quarkus.it.oidc.dev.services;

import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "typesafe-token-propagation")
@AccessToken
public interface TypesafeGraphQLClientWithTokenPropagation {

    @Query
    String principalName();
}
