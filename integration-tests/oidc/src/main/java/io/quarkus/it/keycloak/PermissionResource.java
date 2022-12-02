package io.quarkus.it.keycloak;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.json.JsonString;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
