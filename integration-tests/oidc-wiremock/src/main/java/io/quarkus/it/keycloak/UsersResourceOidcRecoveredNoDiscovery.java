package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.it.keycloak.model.User;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/recovered-no-discovery/api/users")
public class UsersResourceOidcRecoveredNoDiscovery {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/preferredUserName")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User preferredUserName() {
        return new User(((JsonWebToken) identity.getPrincipal()).getClaim("preferred_username"));
    }
}
