package io.quarkus.it.keycloak;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.json.JsonString;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/tenant/{tenant}/api/user")
public class TenantResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> permissions(@PathParam("tenant") String tenant) {
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
