package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/code-flow-token-introspection")
@Authenticated
public class CodeFlowTokenIntrospectionResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    TokenIntrospection tokenIntrospection;

    @GET
    public String access() {
        if (identity.getPrincipal() instanceof JsonWebToken) {
            return identity.getPrincipal().getName();
        } else {
            return identity.getPrincipal().getName() + ":" + tokenIntrospection.getUsername();
        }
    }
}
