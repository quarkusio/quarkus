package io.quarkus.elytron.security.jdbc;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Test JAXRS endpoint with RolesAllowed specified at the class level
 */
@Path("/roles-class")
@RolesAllowed("user")
public class RolesEndpointClassLevel {
    @GET
    public String echo(@Context SecurityContext sec) {
        return "Hello " + sec.getUserPrincipal().getName();
    }

}
