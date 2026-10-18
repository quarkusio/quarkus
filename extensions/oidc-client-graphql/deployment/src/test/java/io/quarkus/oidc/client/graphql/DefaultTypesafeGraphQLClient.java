package io.quarkus.oidc.client.graphql;

import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@GraphQLClientApi(configKey = "typesafe-default")
@OidcClientFilter
public interface DefaultTypesafeGraphQLClient {

    @Query
    String principalName();

}
