package io.quarkus.it.keycloak;

import java.security.Principal;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    Principal principal;

    @GET
    @Produces("text/plain")
    @RolesAllowed("user")
    public Uni<String> principalName() {
        return Uni.createFrom().item(principal.getName());
    }
}
