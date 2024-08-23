package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.keycloak.model.User;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/opaque/api/users")
public class OpaqueUsersResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/me/bearer")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User principalName() {
        return new User(identity.getPrincipal().getName());
    }

    @GET
    @Path("/preferredUserName/bearer")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User opaquePreferredUserName() {
        return new User(identity.getPrincipal().getName());
    }

}
