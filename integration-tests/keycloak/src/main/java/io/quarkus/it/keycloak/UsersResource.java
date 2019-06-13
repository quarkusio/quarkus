package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.keycloak.KeycloakSecurityContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/users")
public class UsersResource {

    @Inject
    KeycloakSecurityContext keycloakSecurityContext;

    @GET
    @Path("/me")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User me() {
        return new User(keycloakSecurityContext);
    }

    public class User {

        private final String userName;

        User(KeycloakSecurityContext securityContext) {
            this.userName = securityContext.getToken().getPreferredUsername();
        }

        public String getUserName() {
            return userName;
        }
    }
}
