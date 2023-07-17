package io.quarkus.it.keycloak;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/permission")
public class PermissionResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{name}")
    @RolesAllowed("user")
    public Map<String, Object> permissions() {
        Map<String, Object> claims = new HashMap<>();
        for (String i : jwt.getClaimNames()) {
            claims.put(i, fromJsonWebObject(jwt.getClaim(i)));
        }
        return claims;
    }

    private Object fromJsonWebObject(Object claim) {
        return claim instanceof JsonString ? ((JsonString) claim).getString() : claim != null ? claim.toString() : null;
    }

}
