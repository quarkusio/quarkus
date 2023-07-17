package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-refresh")
public class TenantRefresh {
    @Inject
    RoutingContext context;

    @Authenticated
    @GET
    public String getTenantRefresh() {
        return "Tenant Refresh, refreshed: " + (context.get("refresh_token_grant_response") != null);
    }
}
