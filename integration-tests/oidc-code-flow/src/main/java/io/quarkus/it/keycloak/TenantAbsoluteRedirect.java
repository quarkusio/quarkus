package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.security.Authenticated;

@Path("/tenant-absolute-redirect")
public class TenantAbsoluteRedirect {

    @Context
    UriInfo ui;

    @GET
    @Authenticated
    public String getTenant() {
        throw new RuntimeException("/tenant-absolute-redirect/callback is a callback method");
    }

    @GET
    @Authenticated
    @Path("/callback")
    public String getTenantCallback() {
        return ui.getAbsolutePath().toString();
    }
}
