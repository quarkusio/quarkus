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

@Tenant("hr")
@Authenticated
@Path("/api/tenant-echo")
public class TenantEchoResource {

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTenant() {
        return OidcUtils.TENANT_ID_ATTRIBUTE + "=" + routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE)
                + ", static.tenant.id=" + routingContext.get("static.tenant.id")
                + ", name=" + identity.getPrincipal().getName();
    }
}
