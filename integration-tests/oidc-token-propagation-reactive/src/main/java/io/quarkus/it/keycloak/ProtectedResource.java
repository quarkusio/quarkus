package io.quarkus.it.keycloak;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
