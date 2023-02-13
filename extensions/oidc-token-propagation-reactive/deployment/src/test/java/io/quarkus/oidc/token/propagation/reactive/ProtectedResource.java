package io.quarkus.oidc.token.propagation.reactive;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed("user")
    public String principalName() {
        return jwt.getName();
    }
}
