package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;

@Path("/roles")
@Authenticated
public class RolesResource {

    @Inject
    JsonWebToken jwt;

    @GET
    public String get() {
        if ("alice".equals(jwt.getName())) {
            return "tester";
        }
        throw new ForbiddenException("Only user 'bob' is allowed to request roles");
    }
}
