package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;

@Path("/api/admin")
public class AdminResource {

    @Claim("preferred_username")
    ClaimValue<String> claim;

    @GET
    @RolesAllowed("admin")
    public String admin() {
        return "granted:" + claim.getValue();
    }
}
