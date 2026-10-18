package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.security.Authenticated;

@Path("/tenant-restore-path-absolute-redirect")
public class TenantRestorePathAbsoluteRedirect {

    @Context
    UriInfo ui;

    @GET
    @Authenticated
    public String getTenant() {
        return ui.getAbsolutePath().toString();
    }

    @GET
    @Authenticated
    @Path("/callback")
    public String getTenantCallback() {
        throw new RuntimeException("/tenant-restore-path-absolute-redirect must be restored");
    }
}
