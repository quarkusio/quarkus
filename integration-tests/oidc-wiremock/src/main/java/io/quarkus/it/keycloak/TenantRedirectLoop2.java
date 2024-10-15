package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.Tenant;
import io.quarkus.security.Authenticated;

@Tenant("redirect-loop2")
@Path("/api-redirect-loop2")
@Authenticated
public class TenantRedirectLoop2 {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTenant() {
        return "redirect-loop2";
    }
}
