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

    @GET
    @Path("tenant-introspection-multiple-required-claims")
    public String userPermission2() {
        var requiredClaimValBuilder = new StringBuilder();
        var requiredClaimArr = token.getArray("required_claim");
        for (int i = 0; i < requiredClaimArr.size(); i++) {
            if (i > 0) {
                requiredClaimValBuilder.append(",");
            }
            requiredClaimValBuilder.append(requiredClaimArr.getString(i));
        }
        return token.getUsername() + ", required_claim:" + requiredClaimValBuilder;
    }
}
