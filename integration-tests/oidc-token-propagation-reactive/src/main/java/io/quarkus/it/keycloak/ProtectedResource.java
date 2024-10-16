package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Produces("text/plain")
    @RolesAllowed("user")
    public Uni<String> principalName() {
        return Uni.createFrom().item(jwt.getClaim("typ") + ":" + jwt.getName());
    }
}
