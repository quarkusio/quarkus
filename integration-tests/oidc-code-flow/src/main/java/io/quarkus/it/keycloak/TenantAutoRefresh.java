package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/tenant-autorefresh")
public class TenantAutoRefresh {
    @Inject
    RoutingContext context;

    @Authenticated
    @GET
    public String getTenantLogout() {
        return "Tenant AutoRefresh, refreshed: " + (context.get("refresh_token_grant_response") != null);
    }
}
