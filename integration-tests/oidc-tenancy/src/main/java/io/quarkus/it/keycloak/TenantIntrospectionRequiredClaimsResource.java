package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.security.Authenticated;

@Path("/tenant-introspection")
@Authenticated
public class TenantIntrospectionRequiredClaimsResource {

    @Inject
    TokenIntrospection token;

    @GET
    @Path("tenant-introspection-required-claims")
    public String userPermission() {
        return token.getUsername() + ", required_claim:" + token.getString("required_claim");
    }
}
