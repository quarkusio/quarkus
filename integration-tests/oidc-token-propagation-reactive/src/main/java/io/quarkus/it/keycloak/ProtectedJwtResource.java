package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

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
