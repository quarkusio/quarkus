package io.quarkus.oidc.token.propagation.graphql;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@GraphQLApi
public class ProtectedGraphQLApi {

    @Inject
    SecurityIdentity identity;

    @Query
    @Authenticated
    public String me() {
        return identity.getPrincipal().getName();
    }
}
