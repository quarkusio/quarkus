package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
