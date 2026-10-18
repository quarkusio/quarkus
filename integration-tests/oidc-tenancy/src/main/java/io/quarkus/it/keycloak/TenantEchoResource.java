package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Tenant("tenant-public-key")
@Path("/api/tenant-echo")
public class TenantEchoResource {

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityIdentity identity;

    @Path("/jax-rs-perm-check")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTenantJaxRsPermCheck() {
        return getTenantInternal();
    }

    @RolesAllowed("role3")
    @Path("/classic-and-jaxrs-perm-check")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTenantClassicAndJaxRsPermCheck() {
        return getTenantInternal();
    }

    @PermissionsAllowed("get-tenant")
    @Path("/hr-identity-augmentation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantIdentityAugmentation() {
        return getTenantInternal();
    }

    private String getTenantInternal() {
        return OidcUtils.TENANT_ID_ATTRIBUTE + "=" + routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE)
                + ", static.tenant.id=" + routingContext.get("static.tenant.id")
                + ", name=" + identity.getPrincipal().getName();
    }
}
