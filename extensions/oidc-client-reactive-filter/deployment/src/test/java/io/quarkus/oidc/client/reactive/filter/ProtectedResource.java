package io.quarkus.oidc.client.reactive.filter;

import java.security.Principal;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/protected")
public class ProtectedResource {

    @Inject
    Principal principal;

    @GET
    @RolesAllowed("user")
    public String principalName() {
        return principal.getName();
    }

    @GET
    @Path("/anonymous")
    public String anonymousPrincipalName() {
        return principal.getName();
    }
}
