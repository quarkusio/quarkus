package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("user")
    public String principalName() {
        return identity.getPrincipal().getName();
    }
}
