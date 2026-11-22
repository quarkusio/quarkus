package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.security.identity.SecurityIdentity;

@GraphQLApi
public class PublicGraphQLApi {

    @Inject
    SecurityIdentity identity;

    @Query
    public String me() {
        return identity.isAnonymous() ? "anonymous" : identity.getPrincipal().getName();
    }
}
