package io.quarkus.oidc.token.propagation.graphql;

import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "propagation-client")
@AccessToken
public interface PropagationTypesafeGraphQLClient {

    @Query
    String me();
}
