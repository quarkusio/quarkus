package io.quarkus.security.jpa.reactive;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RolesAllowed("user")
@Path("/user-secured")
public class SingleRoleSecuredResource {

    @GET
    public String get() {
        return "A secured message";
    }

}
