package io.quarkus.oidc.client.graphql;

import java.security.Principal;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@GraphQLApi
@Authenticated
public class ProtectedResource {

    @Inject
    Principal principal;

    @Inject
    SecurityIdentity securityIdentity;

    @Query
    public String principalName() {
        return principal.getName();
    }

    @Query
    public String accessToken() {
        return securityIdentity.getCredential(AccessTokenCredential.class).getToken();
    }
}
