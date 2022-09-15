package io.quarkus.it.keycloak;

import java.security.Principal;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    Principal principal;

    @GET
    @RolesAllowed("user")
    public String principalName() {
        return principal.getName();
    }
}
