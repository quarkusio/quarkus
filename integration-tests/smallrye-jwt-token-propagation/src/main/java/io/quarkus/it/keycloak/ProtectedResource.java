package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String configuredIssuer;

    @GET
    @RolesAllowed("user")
    public String principalName() {
        checkIssuerAndAudience();
        return jwt.getName();
    }

    private void checkIssuerAndAudience() {
        // it has already been verified by smallrye-jwt
        if (!configuredIssuer.equals(jwt.getIssuer())) {
            throw new NotAuthorizedException(401);
        }
        if (jwt.getAudience() != null) {
            // Keycloak does not set the audience
            throw new NotAuthorizedException(401);
        }
    }
}
