package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/tenant-autorefresh")
public class TenantAutoRefresh {
    @Authenticated
    @GET
    public String getTenantLogout() {
        return "Tenant AutoRefresh";
    }
}
