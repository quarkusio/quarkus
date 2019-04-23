/**
 * Copyright 2019 Red Hat, Inc, and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.example.keycloak;

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
