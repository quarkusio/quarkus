package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/management")
public class ManagementResource {

    @GET
    @Path("/only-user")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "USER" })
    public String hello() {
        return "User Panel ::: Management";
    }
}