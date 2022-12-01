package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
