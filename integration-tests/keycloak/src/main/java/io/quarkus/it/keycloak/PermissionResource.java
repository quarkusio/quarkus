package io.quarkus.it.keycloak;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/permission")
public class PermissionResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{name}")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> permissions() {
        Map<String, Object> claims = new HashMap<>();
        for (String i : jwt.getClaimNames()) {
            claims.put(i, jwt.claim(i));
        }
        return claims;
    }
}
