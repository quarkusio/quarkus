package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Authenticated
@Path("/api/tenant-echo2")
public class TenantEcho2Resource {

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityIdentity identity;

    @Path("/hr")
    @Tenant("hr")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenant() {
        return getTenant();
    }

    @Path("/hr-jax-rs-perm-check")
    @Tenant("hr")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantJaxRsPermCheck() {
        return getTenant();
    }

    @Path("/hr-classic-perm-check")
    @Tenant("hr")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantClassicPermCheck() {
        return getTenant();
    }

    @Path("/hr-classic-and-jaxrs-perm-check")
    @Tenant("hr")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrTenantClassicAndJaxRsPermCheck() {
        return getTenant();
    }

    @Path("/default")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getDefaultTenant() {
        return getTenant();
    }

    private String getTenant() {
        return OidcUtils.TENANT_ID_ATTRIBUTE + "=" + routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE)
                + ", static.tenant.id=" + routingContext.get("static.tenant.id")
                + ", name=" + identity.getPrincipal().getName()
                + ", " + OidcUtils.TENANT_ID_SET_BY_ANNOTATION + "="
                + routingContext.get(OidcUtils.TENANT_ID_SET_BY_ANNOTATION);
    }
}
