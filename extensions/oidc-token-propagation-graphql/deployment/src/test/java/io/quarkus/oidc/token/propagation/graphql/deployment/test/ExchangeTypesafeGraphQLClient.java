package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.mutiny.Uni;

@GraphQLClientApi(configKey = "exchange-client")
@AccessToken(exchangeTokenClient = "Default")
public interface ExchangeTypesafeGraphQLClient {

    @Query
    String me();

    @Query("me")
    Uni<String> meAsync();
}
