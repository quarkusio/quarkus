package io.quarkus.it.keycloak;

import io.quarkus.security.identity.SecurityIdentity;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/users")
public class UsersResource {

    @Inject
    SecurityIdentity keycloakSecurityContext;

    @GET
    @Path("/me")
    public User me() {
        return new User(keycloakSecurityContext);
    }

    public static class User {

        private final String userName;

        User(SecurityIdentity securityContext) {
            this.userName = securityContext.getPrincipal().getName();
        }

        public String getUserName() {
            return userName;
        }
    }
}
