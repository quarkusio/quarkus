package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Tenant("hr")
@Path("/api/tenant-echo")
public class TenantEchoResource {

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityIdentity identity;

    @Authenticated
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTenant() {
        return getTenantInternal();
    }

    @Path("/hr-jax-rs-perm-check")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantJaxRsPermCheck() {
        return getTenantInternal();
    }

    @RolesAllowed("role3")
    @Path("/hr-classic-and-jaxrs-perm-check")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantClassicAndJaxRsPermCheck() {
        return getTenantInternal();
    }

    @PermissionsAllowed("get-tenant")
    @Path("/hr-identity-augmentation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantIdentityAugmentation() {
        return getTenantInternal();
    }

    @Path("/http-security-policy-applies-all-diff")
    @GET
    public String httpSecurityPolicyAppliesAllDiff() {
        throw new IllegalStateException("An exception should have been thrown because authentication happened" +
                " before Tenant was selected with the @Tenant annotation");
    }

    @Path("/http-security-policy-applies-all-same")
    @GET
    public String httpSecurityPolicyAppliesAllSame() {
        return getTenantInternal();
    }

    private String getTenantInternal() {
        return OidcUtils.TENANT_ID_ATTRIBUTE + "=" + routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE)
                + ", static.tenant.id=" + routingContext.get("static.tenant.id")
                + ", name=" + identity.getPrincipal().getName()
                + ", " + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "="
                + routingContext.get(OidcUtils.TENANT_ID_SET_BY_ANNOTATION);
    }
}
