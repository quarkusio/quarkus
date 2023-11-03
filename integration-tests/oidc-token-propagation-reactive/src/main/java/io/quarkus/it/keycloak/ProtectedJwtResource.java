package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/web-app")
@Authenticated
public class ProtectedJwtResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken accessToken;

    @Context
    SecurityContext securityContext;

    @GET
    @Path("test-security")
    @RolesAllowed("viewer")
    public String testSecurity() {
        return securityContext.getUserPrincipal().getName();
    }

    @POST
    @Path("test-security")
    @Consumes("application/json")
    @RolesAllowed("viewer")
    public String testSecurityJson(User user) {
        return user.getName() + ":" + securityContext.getUserPrincipal().getName();
    }

    @GET
    @Path("test-security-jwt")
    @RolesAllowed("viewer")
    public String testSecurityJwt() {
        return accessToken.getName() + ":" + accessToken.getGroups().iterator().next()
                + ":" + accessToken.getClaim("email");
    }
}
