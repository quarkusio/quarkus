package io.quarkus.oidc.token.propagation.graphql;

import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "exchange-client")
@AccessToken(exchangeTokenClient = "Default")
public interface ExchangeTypesafeGraphQLClient {

    @Query
    String me();
}
