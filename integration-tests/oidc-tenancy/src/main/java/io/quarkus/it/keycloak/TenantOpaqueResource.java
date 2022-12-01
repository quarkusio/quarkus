package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/tenant-opaque")
@Authenticated
public class TenantOpaqueResource {

    @Inject
    SecurityIdentity identity;
    @Inject
    AccessTokenCredential accessToken;

    @Inject
    TokenIntrospection tokenIntrospection;

    @GET
    @RolesAllowed("user")
    @Path("tenant-oidc/api/testsecurity")
    public String testSecurity() {
        return "tenant-oidc-opaque:" + identity.getPrincipal().getName();
    }

    @GET
    @RolesAllowed("user")
    @Path("tenant-oidc/api/user")
    public String userName() {
        if (!identity.getCredential(AccessTokenCredential.class).isOpaque()) {
            throw new OIDCException("Opaque token is expected");
        }
        return "tenant-oidc-opaque:" + identity.getPrincipal().getName()
                + ":" + tokenIntrospection.getString("scope")
                + ":" + tokenIntrospection.getString("email");
    }

    @GET
    @Path("tenant-oidc-no-opaque-token/api/user")
    public String userNameNoOpaqueToken() {
        throw new OIDCException("This method must not be invoked because the opaque tokens are not allowed");
    }
}
