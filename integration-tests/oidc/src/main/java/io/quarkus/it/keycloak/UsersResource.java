package io.quarkus.it.keycloak;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/users")
public class UsersResource {

    @Inject
    Principal identity;

    @GET
    @Path("/me")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User principalName() {
        return new User(identity.getName());
    }

    @GET
    @Path("/preferredUserName")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User preferredUserName() {
        return new User(((JsonWebToken) identity).getClaim("preferred_username"));
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
