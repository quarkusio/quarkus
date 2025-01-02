package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.security.Authenticated;

@Path("/code-flow-token-introspection-expires-in")
@Authenticated
public class CodeFlowTokenIntrospectionExpiresInResource {

    @Inject
    TokenIntrospection tokenIntrospection;

    @GET
    public String access() {
        return tokenIntrospection.getUsername();
    }
}
