package io.quarkus.it.security.webauthn;

import java.security.Principal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/public")
public class PublicResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String publicResource() {
        return "public";
    }

    @GET
    @Path("/me")
    @Produces(MediaType.TEXT_PLAIN)
    public String me(@Context SecurityContext securityContext) {
        Principal user = securityContext.getUserPrincipal();
        return user != null ? user.getName() : "<not logged in>";
    }
}
