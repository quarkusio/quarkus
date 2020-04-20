package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/tenant-logout")
public class TenantLogout {

    @Authenticated
    @GET
    public String getTenantLogout() {
        return "Tenant Logout";
    }

    @GET
    @Path("post-logout")
    public String postLogout() {
        return "You were logged out";
    }
}
