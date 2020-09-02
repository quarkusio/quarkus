package io.quarkus.it.keycloak;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
