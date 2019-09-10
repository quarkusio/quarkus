package io.quarkus.it.keycloak;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
    public User me() {
        return new User(identity);
    }

    public class User {

        private final String userName;

        User(Principal securityContext) {
            this.userName = securityContext.getName();
        }

        public String getUserName() {
            return userName;
        }
    }
}
