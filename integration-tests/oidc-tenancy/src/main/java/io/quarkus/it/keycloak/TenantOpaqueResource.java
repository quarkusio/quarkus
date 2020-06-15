package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/tenant-opaque/tenant-oidc/api/user")
@Authenticated
public class TenantOpaqueResource {

    @Inject
    SecurityIdentity identity;
    @Inject
    AccessTokenCredential accessToken;

    @GET
    public String userName(@PathParam("tenant") String tenant) {
        if (!identity.getCredential(AccessTokenCredential.class).isOpaque()) {
            throw new OIDCException("Opaque token is expected");
        }
        return "tenant-oidc-opaque:" + identity.getPrincipal().getName();
    }
}
