package io.quarkus.it.oidc.dev.services;

import java.security.Principal;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.security.Authenticated;

@GraphQLApi
@Authenticated
public class ProtectedGraphQLEndpoint {

    @Inject
    Principal principal;

    @Query
    public String principalName() {
        return principal.getName();
    }
}
