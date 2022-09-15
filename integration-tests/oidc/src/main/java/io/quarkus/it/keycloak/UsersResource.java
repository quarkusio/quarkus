package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/users")
public class UsersResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/me")
    @RolesAllowed("user")
    public User principalName() {
        return new User(identity.getPrincipal().getName());
    }

    @GET
    @Path("/preferredUserName")
    @RolesAllowed("user")
    public User preferredUserName() {
        return new User(((JsonWebToken) identity.getPrincipal()).getClaim("preferred_username"));
    }

    public static class User {

        private final String userName;

        User(String name) {
            this.userName = name;
        }

        public String getUserName() {
            return userName;
        }
    }
}
